import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSortedSet
import com.ibm.wala.classLoader.IClass
import com.ibm.wala.classLoader.IField
import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.types.TypeName
import com.ibm.wala.types.TypeReference
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.internal.Actions
import org.gradlex.plugins.analyzer.Analysis
import org.gradlex.plugins.analyzer.Analyzer
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import org.gradlex.plugins.analyzer.Reporter
import org.gradlex.plugins.analyzer.WalaUtil.toFQCN
import org.gradlex.plugins.analyzer.TypeRepository
import org.gradlex.plugins.analyzer.TypeRepository.TypeSet
import org.gradlex.plugins.analyzer.TypeRepository.TypeSet.ALL_EXTERNAL_REFERENCED_TYPES
import org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES
import org.gradlex.plugins.analyzer.analysis.ShouldNotReferenceInternalApi
import org.gradlex.plugins.analyzer.analysis.TypeShouldExtendType
import org.gradlex.plugins.analyzer.analysis.TypeShouldNotOverrideGetter
import org.gradlex.plugins.analyzer.analysis.TypeShouldNotOverrideSetter
import org.jsoup.Jsoup
import org.slf4j.event.Level
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.SortedSet

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
data class Message(val level: String, val message: String) : Comparable<Message> {
    override fun compareTo(other: Message) = message.compareTo(other.message)
}

@Serializable
data class MessageGroup(val title: String, val messages: List<Message>)

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
            val messageGroups = mutableListOf<MessageGroup>()
            val analyzer = DefaultAnalyzer(TypeRepository(files)) { arg ->
                when (arg) {
                    is IClass -> formatType(arg.name)
                    is TypeReference -> formatType(arg.name)
                    is TypeName -> formatType(arg)
                    is IField -> formatField(arg)
                    is IMethod -> formatMethod(arg)
                    else -> arg.toString()
                }
            }

            fun Analyzer.analyze(title: String, set: TypeSet, analysis: Analysis) {
                val builder = ImmutableSortedSet.naturalOrder<Message>()
                analyze(set, analysis, Reporter { level, message, args ->
                    if (level.toInt() >= parameters.level.get().toInt()) {
                        builder.add(Message(level.name, message.format(*args)))
                    }
                })
                val messages = builder.build()
                if (!messages.isEmpty()) {
                    messageGroups.add(MessageGroup(title, messages.asList()))
                }
            }

            analyzer.analyze("Task should extend DefaultTask", EXTERNAL_TASK_TYPES, TypeShouldExtendType("Lorg/gradle/api/DefaultTask"))
            analyzer.analyze("Should not override setter", ALL_EXTERNAL_REFERENCED_TYPES, TypeShouldNotOverrideSetter())
            analyzer.analyze("Should not override getter", ALL_EXTERNAL_REFERENCED_TYPES, TypeShouldNotOverrideGetter())
            analyzer.analyze("Should not reference internal Gradle API", ALL_EXTERNAL_REFERENCED_TYPES, ShouldNotReferenceInternalApi())

            val report = parameters.reportFile.get().asFile
            FileOutputStream(report).use { stream ->
                Json.encodeToStream(messageGroups, stream)
            }
        }

        private fun formatType(type: TypeName) = "type `${toFQCN(type.toString())}`"
        private fun formatField(field: IField) = "field `${toFQCN(field.declaringClass)}.${field.name}`"
        private fun formatMethod(method: IMethod): String {
            val paramTypes = mutableListOf<String>()
            // Ignore 'this"
            val firstParam = if (method.isStatic) 0 else 1
            for (iParam in firstParam until method.numberOfParameters) {
                // Let's use the simple name of parameter types
                paramTypes += method.getParameterType(iParam).name.className.toString()
            }
            return "method `${toFQCN(method.declaringClass)}.${method.name}(${paramTypes.joinToString(",")})`"
        }
    }

    @TaskAction
    fun execute() {
        val task = this
        workerExecutor.processIsolation {
            forkOptions {
                // Seems to help analyze Kotlin plugins quite a bit
                maxHeapSize = "1g"
            }
        }
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
            writer.println("## Plugin [`${pluginId.get()}`](https://plugins.gradle.org/plugin/${pluginId.get()})")
            writer.println()

            val messageGroups = FileInputStream(inputFile).use { stream ->
                Json.decodeFromStream<List<MessageGroup>>(stream)
            }
            if (messageGroups.isNotEmpty()) {
                messageGroups.forEach { messageGroup ->
                    writer.println("### ${messageGroup.title}")
                    writer.println()
                    messageGroup.messages.forEach { message ->
                        val levelSymbol = when (message.level) {
                            "INFO" -> "\uD83D\uDCAC"
                            "WARN" -> "⚠\uFE0F"
                            "ERROR" -> "❌"
                            else -> "❓"
                        }
                        writer.println("- ${levelSymbol} ${message.message}")
                    }
                    writer.println()
                }
            } else {
                writer.println("No messages generated. Good plugin! ❤\uFE0F")
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
            writer.println("# Plugin validation report")
            writer.println()
            writer.println("Produced at **${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}**")
            writer.println()

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
