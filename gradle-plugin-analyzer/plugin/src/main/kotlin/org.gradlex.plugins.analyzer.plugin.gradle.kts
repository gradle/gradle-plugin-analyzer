import org.gradle.internal.Actions
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotExtendDefaultTask
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotOverrideGetter
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotOverrideSetter
import org.jsoup.Jsoup
import org.slf4j.event.Level
import java.io.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

plugins {
    // Required since we are resolving JVM artifacts
    `jvm-ecosystem`
}

open class AnalyzedPlugin(val pluginId: String) : Comparable<AnalyzedPlugin>, Named, Serializable {

    var artifact: String? = null

    var sourceUrl: String? = null

    override fun getName() = pluginId

    override fun compareTo(other: AnalyzedPlugin) = pluginId.compareTo(other.pluginId)
}

open class PluginAnalyzerExtension(objects: ObjectFactory) : Serializable {
    val analyzedPlugins = objects.domainObjectContainer(AnalyzedPlugin::class.java)

    fun plugin(artifact: String, configuration: Action<in AnalyzedPlugin> = Actions.doNothing()) {
        analyzedPlugins.create(artifact, configuration)
    }
}

@CacheableTask
abstract class PluginAnalyzerTask : DefaultTask() {
    @get:Input
    abstract val pluginId: Property<String>

    @get:Input
    @get:Optional
    abstract val pluginSourceUrl: Property<String>

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Classpath
    abstract val gradleApi: RegularFileProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val level: Property<Level>

    init {
        level.convention(Level.INFO)
    }

    @TaskAction
    fun execute() {
        val files = mutableListOf<Path>()
        files.add(gradleApi.get().asFile.toPath())
        classpath.files.stream()
            .map(File::toPath)
            .forEach(files::add)

        val report = reportFile.get().asFile
        report.delete()
        report.createNewFile()

        report.appendText("## ${pluginId.get()}\n\n")
        report.appendText("Classpath:\n")
        classpath.files.forEach { entry ->
            report.appendText("- `$entry\n")
        }
        report.appendText("\n")

        val analyzer = DefaultAnalyzer(files) { messageLevel, message ->
            if (messageLevel.toInt() >= level.get().toInt()) {
                report.appendText("- $messageLevel: $message\n")
            }
        }

        analyzer.analyze(TaskImplementationDoesNotExtendDefaultTask())
        analyzer.analyze(TaskImplementationDoesNotOverrideSetter())
        analyzer.analyze(TaskImplementationDoesNotOverrideGetter())
    }

    fun formatPlugin(artifact: String, sourceUrl: String?): String {
        val title = "`${artifact}`"
        if (sourceUrl != null) {
            return "[$title](${sourceUrl})"
        } else {
            return title
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
        report.delete()
        report.createNewFile()

        inputReports.files.toSortedSet().forEach { inputReport ->
            report.appendText(inputReport.readText())
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

afterEvaluate {
    pluginAnalyzer.analyzedPlugins.forEach { analyzedPlugin ->
        val simplifiedName = analyzedPlugin.pluginId.replace(':', '_').replace('.', '_')

        val config = configurations.create("conf_$simplifiedName")
        configureRequestAttributes(config)
        config.dependencies.add(dependencies.create(analyzedPlugin.pluginId + ":" + analyzedPlugin.pluginId + ".gradle.plugin:+"))

        val task = tasks.register<PluginAnalyzerTask>("analyze_$simplifiedName") {
            pluginId = analyzedPlugin.pluginId
            pluginSourceUrl = "https://plugins.gradle.org/plugin/${analyzedPlugin.pluginId}"
            classpath = config
            gradleApi = project.file("${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
            reportFile = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.md")
        }

        analyzePluginsTask.configure {
            inputReports.from(task.flatMap { it.reportFile })
        }
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
