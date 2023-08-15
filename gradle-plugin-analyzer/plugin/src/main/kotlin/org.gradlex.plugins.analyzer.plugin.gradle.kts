import org.gradlex.plugins.analyzer.DefaultAnalyzer
import org.gradlex.plugins.analyzer.TaskImplementationDoesNotExtendDefaultTask
import org.slf4j.event.Level
import java.nio.file.Path

open class PluginAnalyzerExtension(objects: ObjectFactory) {
    val plugins = objects.domainObjectSet(String::class.java)
}

abstract class PluginAnalyzerTask : DefaultTask() {
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

        val analyzer = DefaultAnalyzer(files) { messageLevel, message ->
            if (messageLevel.toInt() >= level.get().toInt()) {
                report.appendText("$messageLevel: $message\n")
            }
        }

        analyzer.analyze(TaskImplementationDoesNotExtendDefaultTask())
    }
}

abstract class PluginAnalysisCollectorTask : DefaultTask() {
    @get:InputFiles
    abstract val inputReports: ConfigurableFileCollection

    @get:OutputFile
    abstract val aggregateReportFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val report = aggregateReportFile.get().asFile
        report.delete()
        report.createNewFile()

        inputReports.files.forEach { inputReport ->
            report.appendText(inputReport.readText())
        }
    }
}

val analyzePluginsTask = tasks.register<PluginAnalysisCollectorTask>("analyzePlugins") {
    aggregateReportFile = project.layout.buildDirectory.file("plugin-analysis/aggregate-report.txt")
}

val pluginAnalyzer = extensions.create<PluginAnalyzerExtension>("pluginAnalyzer")

pluginAnalyzer.plugins.all {
    val plugin = this
    val simplifiedName = plugin.replace(':', '_').replace('.', '_')
    val config = configurations.create("conf_$simplifiedName")
    dependencies.add(config.name, plugin)

    val task = tasks.register<PluginAnalyzerTask>("analyze_$simplifiedName") {
        classpath = config
        gradleApi = project.file("${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
        reportFile = project.layout.buildDirectory.file("plugin-analysis/plugins/report-${simplifiedName}.txt")
    }

    analyzePluginsTask.configure {
        inputReports.from(task.flatMap { it.reportFile })
    }
}
