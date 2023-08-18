import com.google.common.collect.MultimapBuilder
import org.gradle.api.internal.lambdas.SerializableLambdas
import org.gradle.api.internal.project.ProjectInternal
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
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

open class AnalyzedPlugin(val pluginId: String) : Comparable<AnalyzedPlugin>, Named, Serializable {

    var artifact: String? = null

    var sourceUrl: String? = null

    internal var shadowed = false

    internal var configurator: Action<in Configuration> = Actions.doNothing()

    override fun getName() = pluginId
    fun configuration(configurator: SerializableLambdas.SerializableAction<in Configuration>) {
        this.configurator = configurator
    }

    fun shadowed() {
        shadowed = true
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
    abstract val pluginIds: ListProperty<String>

    @get:Input
    abstract val pluginArtifact: Property<String>

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
        report.appendText("## ${formatPlugin(pluginArtifact.get(), pluginSourceUrl.orNull)}\n\n")
        report.appendText("Plugin IDs: `${pluginIds.get().joinToString("`, `")}`\n\n")

        val analyzer = DefaultAnalyzer(files) { messageLevel, message ->
            if (messageLevel.toInt() >= level.get().toInt()) {
                report.appendText("- $messageLevel: $message\n")
            }
        }

        analyzer.analyze(TaskImplementationDoesNotExtendDefaultTask())
        analyzer.analyze(TaskImplementationDoesNotOverrideSetter())
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
    // Resolve artifacts
    val pluginInfoFutures = pluginAnalyzer.analyzedPlugins.stream()
        .map { analyzedPlugin ->
            val artifact: String? = analyzedPlugin.artifact
            if (artifact == null) {
                CompletableFuture.supplyAsync {
                    val info = Util.lookupPlugin(analyzedPlugin.pluginId)
                    analyzedPlugin.artifact = info.artifact
                    Pair(info, analyzedPlugin)
                }
            } else {
                CompletableFuture.completedFuture(Pair(PluginInfo(artifact, null), analyzedPlugin))
            }
        }
        .toList()
    CompletableFuture.allOf(*pluginInfoFutures.toTypedArray()).join()

    val analyzedPlugins = MultimapBuilder.treeKeys().arrayListValues().build<PluginInfo, AnalyzedPlugin>()
    pluginInfoFutures.forEach {
        val pluginInfo = it.get().first
        val plugin = it.get().second
        analyzedPlugins.put(pluginInfo, plugin)
    }

    pluginAnalyzer.analyzedPlugins.forEach { analyzedPlugin ->
    }

    // Must run in afterEvaluate to get the actual configuration of the elements in the container; all() triggers too early
    analyzedPlugins.asMap().forEach { info, plugins ->
        val simplifiedName = info.artifact.replace(':', '_').replace('.', '_')
        val config = configurations.create("conf_$simplifiedName")
        // Magic by Justin
        (project as ProjectInternal).services.get(JvmPluginServices::class.java).configureAsRuntimeClasspath(config)
        if (plugins.any { it.shadowed }) {
            config.attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
        plugins.forEach { it.configurator.execute(config) }
        dependencies.add(config.name, info.artifact)

        val task = tasks.register<PluginAnalyzerTask>("analyze_$simplifiedName") {
            pluginIds = plugins.map { it.pluginId }
            pluginArtifact = info.artifact
            pluginSourceUrl = info.sourceUrl
            classpath = config
            gradleApi = project.file("${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
            reportFile = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.md")
        }

        analyzePluginsTask.configure {
            inputReports.from(task.flatMap { it.reportFile })
        }
    }
}
