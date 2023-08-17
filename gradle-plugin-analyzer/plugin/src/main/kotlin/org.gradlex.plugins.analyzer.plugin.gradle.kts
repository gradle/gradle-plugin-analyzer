import org.gradle.api.internal.lambdas.SerializableLambdas
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.internal.Actions
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotExtendDefaultTask
import org.gradlex.plugins.analyzer.analysis.TaskImplementationDoesNotOverrideSetter
import org.slf4j.event.Level
import java.io.Serializable
import java.nio.file.Path

open class AnalyzedPlugin(@get:Input val artifact: String) : Comparable<AnalyzedPlugin>, Named, Serializable {

    @get:Input
    internal var title: String = artifact

    @get:Internal("Should be an input, but input snapshotting barfs on it")
    internal var configurator: Action<in Configuration> = Actions.doNothing()

    @Internal
    override fun getName() = artifact

    fun configuration(configurator: SerializableLambdas.SerializableAction<in Configuration>) {
        this.configurator = configurator
    }

    override fun compareTo(other: AnalyzedPlugin) = artifact.compareTo(other.artifact)
}

open class PluginAnalyzerExtension(objects: ObjectFactory) : Serializable {
    val analyzedPlugins = objects.domainObjectContainer(AnalyzedPlugin::class.java)

    fun plugin(artifact: String, configuration: Action<in AnalyzedPlugin> = Actions.doNothing()) {
        analyzedPlugins.create(artifact, configuration)
    }
}

@CacheableTask
abstract class PluginAnalyzerTask : DefaultTask() {
    @get:Nested
    abstract val plugin: Property<AnalyzedPlugin>

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
        report.appendText("## ${plugin.get().title}\n\n")

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

val analyzePluginsTask = tasks.register<PluginAnalysisCollectorTask>("analyzePlugins") {
    aggregateReportFile = project.layout.buildDirectory.file("plugin-analysis/aggregate-report.md")
}

val pluginAnalyzer = extensions.create<PluginAnalyzerExtension>("pluginAnalyzer")

afterEvaluate {
    // Must run in afterEvaluate to get the actual configuration of the elements in the container; all() triggers too early
    pluginAnalyzer.analyzedPlugins.forEach { analyzedPlugin ->
        val simplifiedName = analyzedPlugin.artifact.replace(':', '_').replace('.', '_')
        val config = configurations.create("conf_$simplifiedName")
        // Magic by Justin
        (project as ProjectInternal).services.get(JvmPluginServices::class.java).configureAsRuntimeClasspath(config)
        analyzedPlugin.configurator.execute(config)
        dependencies.add(config.name, analyzedPlugin.artifact)

        val task = tasks.register<PluginAnalyzerTask>("analyze_$simplifiedName") {
            plugin = analyzedPlugin
            classpath = config
            gradleApi = project.file("${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
            reportFile = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.md")
        }

        analyzePluginsTask.configure {
            inputReports.from(task.flatMap { it.reportFile })
        }
    }
}
