import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("cn.hutool:hutool-all:5.8.23")
    implementation("org.apache.commons:commons-text:1.9")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        when (providers.gradleProperty("platformType").get()) {
            "IC" -> intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
            "IU" -> intellijIdeaUltimate(providers.gradleProperty("platformVersion"))
            "IDEA" -> intellijIdea(providers.gradleProperty("platformVersion"))
            else -> throw GradleException("Unsupported platformType. Use IDEA, IC, or IU.")
        }

        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map {
                it.split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
            },
        )

        plugins(
            providers.gradleProperty("platformPlugins").map {
                it.split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
            },
        )

        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    instrumentCode = false

    pluginConfiguration {
        version = providers.gradleProperty("version")

        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog
        changeNotes = providers.gradleProperty("version").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            val pluginUntilBuild = providers.gradleProperty("pluginUntilBuild")
            if (pluginUntilBuild.isPresent && pluginUntilBuild.get().isNotBlank()) {
                untilBuild = pluginUntilBuild
            }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("version").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    val defaultTest = named<Test>("test")

    withType<Test> {
        // IntelliJ test setup resolves the platform and cleans per-task IDE directories in doFirst.
        // Those actions capture Gradle project state and therefore cannot be serialized safely.
        notCompatibleWithConfigurationCache("IntelliJ test setup uses runtime project state")
        useJUnit()
        jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing.text.html.parser=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
            "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-exports=java.desktop/sun.font=ALL-UNNAMED",
        )
        systemProperty("idea.system.path", layout.buildDirectory.dir("idea-test/$name/system").get().asFile.absolutePath)
        systemProperty("idea.config.path", layout.buildDirectory.dir("idea-test/$name/config").get().asFile.absolutePath)
        systemProperty("idea.log.path", layout.buildDirectory.dir("idea-test/$name/log").get().asFile.absolutePath)
        doFirst {
            delete(layout.buildDirectory.dir("idea-test/$name"))
            val platformHome = configurations.named("intellijPlatformDependency").get().singleFile
            val jnaArch = when (System.getProperty("os.arch")) {
                "aarch64", "arm64" -> "aarch64"
                else -> "x86-64"
            }
            systemProperty("jna.boot.library.path", platformHome.resolve("lib/jna/$jnaArch").absolutePath)
        }
    }

    test {
        description = "Runs fast unit tests."
        exclude("**/*IntegrationTest.class", "**/*UiTest.class")
    }

    runIde {
        jvmArgs("--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED")
        jvmArgs("--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED")
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    register<Test>("unitTest") {
        description = "Runs fast unit tests that do not boot the IntelliJ Platform UI."
        group = "verification"
        dependsOn("prepareTest")
        testClassesDirs = defaultTest.get().testClassesDirs
        classpath = defaultTest.get().classpath
        exclude("**/*IntegrationTest.class", "**/*UiTest.class")
    }

    register<Test>("integrationTest") {
        description = "Runs IntelliJ Platform integration tests."
        group = "verification"
        dependsOn("prepareTest")
        testClassesDirs = defaultTest.get().testClassesDirs
        classpath = defaultTest.get().classpath
        systemProperty("idea.load.plugins", "false")
        include("**/*IntegrationTest.class")
    }

    register<Test>("ideaUiTest") {
        description = "Runs headless IntelliJ UI component tests."
        group = "verification"
        dependsOn("prepareTest")
        testClassesDirs = defaultTest.get().testClassesDirs
        classpath = defaultTest.get().classpath
        systemProperty("idea.load.plugins", "false")
        // Match GitHub Actions ubuntu-latest (no display) so AWT HeadlessToolkit issues fail locally too.
        systemProperty("java.awt.headless", "true")
        include("**/*UiTest.class")
    }

    register("verifyPluginFast") {
        group = "verification"
        description = "Plugin structure and project configuration checks without Plugin Verifier extra IDEs."
        dependsOn("verifyPluginStructure", "verifyPluginProjectConfiguration")
    }

    check {
        dependsOn("unitTest", "integrationTest", "ideaUiTest")
    }
}
