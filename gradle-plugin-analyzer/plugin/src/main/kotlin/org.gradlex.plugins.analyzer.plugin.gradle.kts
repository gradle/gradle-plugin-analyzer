import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.internal.Actions
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotExtendDefaultTask
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotOverrideGetter
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotOverrideSetter
import org.gradlex.plugins.analyzer.analysis.TaskImplementationReferencesInternalApi
import org.jsoup.Jsoup
import org.slf4j.event.Level
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

plugins {
    // Required since we are resolving JVM artifacts
    `jvm-ecosystem`
}

val gradleRuntime = configurations.create("gradleRuntime")

dependencies {
    gradleRuntime(gradleApi())
}

abstract class AnalyzedPlugin(val pluginId: String) : Comparable<AnalyzedPlugin>, Named {

    abstract val coordinates: Property<String>

    override fun getName() = pluginId

    override fun compareTo(other: AnalyzedPlugin) = pluginId.compareTo(other.pluginId)
}

open class PluginAnalyzerExtension(objects: ObjectFactory) {
    val analyzedPlugins = objects.domainObjectContainer(AnalyzedPlugin::class.java)

    fun plugin(artifact: String, configuration: Action<in AnalyzedPlugin> = Actions.doNothing()) {
        analyzedPlugins.create(artifact, configuration)
    }
}

@Serializable
data class Message(val level: String, val message: String)

@CacheableTask
abstract class PluginAnalyzerTask : DefaultTask() {
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Classpath
    abstract val runtime: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val level: Property<Level>

    init {
        level.convention(Level.INFO)
    }

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    abstract class Work : WorkAction<Work.Params> {
        interface Params : WorkParameters {
            val classpath: ConfigurableFileCollection

            val runtime: ConfigurableFileCollection

            val reportFile: RegularFileProperty

            val level: Property<Level>
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun execute() {
            val files = parameters.runtime.files.map(File::toPath) +
                parameters.classpath.files.map(File::toPath)
            val messages = mutableListOf<Message>()
            val analyzer = DefaultAnalyzer(files) { messageLevel, message ->
                if (messageLevel.toInt() >= parameters.level.get().toInt()) {
                    messages.add(Message(messageLevel.name, message))
                }
            }

            analyzer.analyze(TaskImplementationDoesNotExtendDefaultTask())
            analyzer.analyze(TaskImplementationDoesNotOverrideSetter())
            analyzer.analyze(TaskImplementationDoesNotOverrideGetter())
            analyzer.analyze(TaskImplementationReferencesInternalApi())

            val report = parameters.reportFile.get().asFile
            FileOutputStream(report).use { stream ->
                Json.encodeToStream(messages, stream)
            }
        }
    }

    @TaskAction
    fun execute() {
        val task = this
        workerExecutor.processIsolation()
            .submit(Work::class) {
                classpath = task.classpath
                runtime = task.runtime
                reportFile = task.reportFile
                level = task.level
            }

        workerExecutor.await()
    }
}

@CacheableTask
abstract class FormatReportTask : DefaultTask() {
    @get:Input
    abstract val pluginId: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jsonReport: RegularFileProperty

    @get:OutputFile
    abstract val markdownReport: RegularFileProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun execute() {
        val inputFile = jsonReport.get().asFile
        val outputFile = markdownReport.get().asFile

        PrintWriter(outputFile).use { writer ->
            writer.println("## [`${pluginId.get()}`](https://plugins.gradle.org/plugin/${pluginId.get()})")
            writer.println()

            val messages = FileInputStream(inputFile).use { stream ->
                Json.decodeFromStream<List<Message>>(stream)
            }
            if (messages.isNotEmpty()) {
                messages.forEach { message ->
                    writer.println("- ${message.level}: ${message.message}")
                }
            } else {
                writer.println("No messages generated. Good plugin!")
            }
        }
    }
}

@CacheableTask
abstract class PluginAnalysisCollectorTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputReports: ConfigurableFileCollection

    @get:OutputFile
    abstract val aggregateReportFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val report = aggregateReportFile.get().asFile
        PrintWriter(report).use { writer ->
            inputReports.files.forEach { inputReport ->
                writer.println(inputReport.readText())
            }
        }

        println("Plugin analysis report: " + report.absolutePath)
    }
}

open class PluginInfo(val artifact: String, val sourceUrl: String?) : Comparable<PluginInfo> {
    override fun compareTo(other: PluginInfo) = artifact.compareTo(other.artifact)
}

companion object Util {
    fun lookupPlugin(pluginId: String): PluginInfo {
        val client = HttpClient.newHttpClient()

        // Create an HttpRequest using the source ID
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://plugins.gradle.org/plugin/$pluginId"))
            .build()

        // Send the HTTP request
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // Parse the HTML into a DOM
        val document = Jsoup.parse(response.body())

        // Find the <pre> tag
        val preElement = document.select("#kotlin-usage > pre:nth-child(4)").first()

        val artifact: String
        if (preElement != null) {
            val kotlinCode = preElement.text()

            val gavRegex = """classpath\("(.+?)"\)""".toRegex()
            val gavMatch = gavRegex.find(kotlinCode)

            if (gavMatch == null) {
                throw RuntimeException("No target ID found for source ID $pluginId")
            }
            artifact = gavMatch.groupValues[1]
            println("Analyzing plugin ${pluginId} with artifact '${artifact}'")
        } else {
            throw RuntimeException("No <pre> tag found for source ID $pluginId")
        }

        val sourceUrlElement = document.select("#content > div > div.row.plugin-detail.box > div.detail > p > a").first()
        val sourceUrl = sourceUrlElement?.attr("href")

        return PluginInfo(artifact, sourceUrl)
    }
}

val analyzePluginsTask = tasks.register<PluginAnalysisCollectorTask>("analyzePlugins") {
    aggregateReportFile = project.layout.buildDirectory.file("plugin-analysis/aggregate-report.md")
}

val pluginAnalyzer = extensions.create<PluginAnalyzerExtension>("pluginAnalyzer")

pluginAnalyzer.analyzedPlugins.all {
    val simplifiedName = pluginId.replace(':', '_').replace('.', '_')

    val config = configurations.create("conf_$simplifiedName")
    configureRequestAttributes(config)

    val defaultCoordinates = "$pluginId:$pluginId.gradle.plugin:latest.release"
    config.dependencies.addLater(
        coordinates.orElse(defaultCoordinates).map { dependencyFactory.create(it) }
    )

    val analyzeTask = tasks.register<PluginAnalyzerTask>("analyze_$simplifiedName") {
        classpath = config
        runtime.from(gradleRuntime)
        reportFile = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.json")
    }

    val analyzedId = pluginId
    val formatterTask = tasks.register<FormatReportTask>("format_$simplifiedName") {
        pluginId = analyzedId
        jsonReport = analyzeTask.flatMap { it.reportFile }
        markdownReport = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.md")
    }

    analyzePluginsTask.configure {
        inputReports.from(formatterTask.flatMap { it.markdownReport })
    }
}

/**
 * Use the same request attribute that Gradle will use to resolve the plugin classpath, as defined by:
 * [Link](https://github.com/gradle/gradle/blob/master/subprojects/core/src/main/java/org/gradle/api/internal/initialization/DefaultScriptClassPathResolver.java#L52-L67)
 *
 * Note, that Gradle also sets the `TargetJvmVersion` attribute to the version of the JVM that this build is
 * running on. However, this would restrict the analyzer plugin to only resolve plugins that have a target Java
 * version less than or equal to the current JVM version.
 * Since the analyzer logic can seemingly run on a lower JVM version than the plugins that it analyzes,
 * we deviate from the conventional attributes by not setting the target JVM attribute.
 */
fun configureRequestAttributes(config: Configuration) {
    config.attributes {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class.java, LibraryElements.JAR))
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
        attributes.attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(GradlePluginApiVersion::class.java, GradleVersion.current().version))
    }
}
