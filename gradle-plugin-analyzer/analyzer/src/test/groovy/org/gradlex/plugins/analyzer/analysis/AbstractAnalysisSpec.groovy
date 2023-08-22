package org.gradlex.plugins.analyzer.analysis

import com.google.common.collect.ImmutableList
import org.codehaus.groovy.control.CompilerConfiguration
import org.gradlex.plugins.analyzer.Analyzer
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import spock.lang.Specification

import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import java.nio.file.Path
import java.nio.file.Paths

class AbstractAnalysisSpec extends Specification {
    String gradleApi
    String localGroovy
    List<String> reports
    List<Path> files
    Analyzer analyzer
    File targetDirectory = new File("build/test-classes/${getClass().simpleName}")

    def setup() {
        assert targetDirectory.deleteDir()

        gradleApi = System.getProperty("gradle-api")
        localGroovy = System.getProperty("local-groovy")
        files = [Paths.get(gradleApi), targetDirectory.toPath()]
        reports = []
    }

    protected Analyzer getAnalyzer() {
        analyzer = new DefaultAnalyzer(files, { level, message ->
            reports += "$level: $message" as String
        })
    }

    protected List<String> getReports() {
        ImmutableList.sortedCopyOf(reports)
    }

    protected void compileJava(String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler()

        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///HelloWorld.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source
            }
        }

        assert compiler.getTask(
            null,
            null,
            null,
            ImmutableList.of(
                "-d", targetDirectory.absolutePath,
                "-classpath", localGroovy + File.pathSeparator + gradleApi
            ),
            null,
            ImmutableList.of(file))
            .call()
    }

    protected void compileGroovy(String source) {
        CompilerConfiguration config = new CompilerConfiguration()
        config.setTargetDirectory(targetDirectory)
        GroovyClassLoader classLoader
        classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, config)
        classLoader.addClasspath(gradleApi)

        classLoader.parseClass(source)
    }
}
