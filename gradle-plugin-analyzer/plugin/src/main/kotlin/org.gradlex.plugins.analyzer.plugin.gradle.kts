import org.gradle.api.internal.lambdas.SerializableLambdas
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.internal.Actions
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotExtendDefaultTask
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotOverrideSetter
import org.jsoup.Jsoup
import org.slf4j.event.Level
import java.io.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

open class AnalyzedPlugin(val pluginId: String) : Comparable<AnalyzedPlugin>, Named, Serializable {

    private var _artifact: String? = null

    var artifact: String
        get() {
            if (_artifact == null) {
                _artifact = Util.findArtifact(pluginId)
            }
            return _artifact!!
        }
        set(artifact) {
            _artifact = artifact
        }

    var shadowed = false

    internal var configurator: Action<in Configuration> = Actions.doNothing()

    override fun getName() = pluginId
    fun configuration(configurator: SerializableLambdas.SerializableAction<in Configuration>) {
        this.configurator = configurator
    }

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
    abstract val pluginArtifact: Property<String>

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
        report.appendText("Artifact: `${pluginArtifact.get()}`\n\n")

        val analyzer = DefaultAnalyzer(files) { messageLevel, message ->
            if (messageLevel.toInt() >= level.get().toInt()) {
                report.appendText("- $messageLevel: $message\n")
            }
        }

        analyzer.analyze(TaskImplementationDoesNotExtendDefaultTask())
        analyzer.analyze(TaskImplementationDoesNotOverrideSetter())
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

companion object Util {
    fun findArtifact(pluginId: String): String {
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

        if (preElement != null) {
            val kotlinCode = preElement.text()

            val gavRegex = """classpath\("(.+?)"\)""".toRegex()
            val gavMatch = gavRegex.find(kotlinCode)

            if (gavMatch != null) {
                val artifact = gavMatch.groupValues[1]
                println("Analyzing plugin ${pluginId} with artifact '${artifact}'")
                return artifact
            } else {
                throw RuntimeException("No target ID found for source ID $pluginId")
            }
        } else {
            throw RuntimeException("No <pre> tag found for source ID $pluginId")
        }
    }
}

val analyzePluginsTask = tasks.register<PluginAnalysisCollectorTask>("analyzePlugins") {
    aggregateReportFile = project.layout.buildDirectory.file("plugin-analysis/aggregate-report.md")
}

val pluginAnalyzer = extensions.create<PluginAnalyzerExtension>("pluginAnalyzer")

afterEvaluate {
    // Must run in afterEvaluate to get the actual configuration of the elements in the container; all() triggers too early
    pluginAnalyzer.analyzedPlugins.forEach { analyzedPlugin ->
        val simplifiedName = analyzedPlugin.name.replace(':', '_').replace('.', '_')
        val config = configurations.create("conf_$simplifiedName")
        // Magic by Justin
        (project as ProjectInternal).services.get(JvmPluginServices::class.java).configureAsRuntimeClasspath(config)
        if (analyzedPlugin.shadowed) {
            config.attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
        analyzedPlugin.configurator.execute(config)
        dependencies.add(config.name, analyzedPlugin.artifact)

        val task = tasks.register<PluginAnalyzerTask>("analyze_$simplifiedName") {
            pluginId = analyzedPlugin.pluginId
            pluginArtifact = analyzedPlugin.artifact
            classpath = config
            gradleApi = project.file("${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
            reportFile = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.md")
        }

        analyzePluginsTask.configure {
            inputReports.from(task.flatMap { it.reportFile })
        }
    }
}
