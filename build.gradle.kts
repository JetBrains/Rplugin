import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import com.google.protobuf.gradle.*

val grpcVersion = "1.75.0"
val protobufVersion = "3.25.1"

plugins {
  id("java") // Java support
  alias(libs.plugins.kotlin) // Kotlin support
  alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
  alias(libs.plugins.changelog) // Gradle Changelog Plugin
  alias(libs.plugins.qodana) // Gradle Qodana Plugin
  alias(libs.plugins.kover) // Gradle Kover Plugin
  id("com.google.protobuf") version "0.9.4"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
  jvmToolchain(17)
}

sourceSets {
  main {
    java.srcDirs("src", "gen", "psi/src", "psi/gen", "psi/gen-grpc/java", "lsp/src")
    resources.srcDirs("resources", "resources-gradle", "psi/resources", "lsp/resources")
    proto {
      srcDir("protos")
      srcDir("grammars")
    }
  }
  test {
    java.srcDirs("test")
    resources.srcDirs("testData", "testResources")
  }
}

// Configure project's dependencies
repositories {
  mavenCentral()

  // IntelliJ Platform Gradle Plugin Repositories Extension
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  testImplementation(libs.junit)
  testImplementation(libs.opentest4j)

  implementation("com.google.protobuf:protobuf-java:$protobufVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  testImplementation("org.assertj:assertj-core:3.18.1")
  testImplementation("org.mockito:mockito-all:1.10.19")
  runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")

  // IntelliJ Platform Gradle Plugin Dependencies Extension
  intellijPlatform {
    intellijIdea(providers.gradleProperty("platformVersion"))

    bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',').map(String::trim).filter(String::isNotBlank) })
    plugins(providers.gradleProperty("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotBlank) })
    bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',').map(String::trim).filter(String::isNotBlank) })

    testFramework(TestFrameworkType.Platform)
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protobufVersion"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
    }
  }
  generateProtoTasks {
    ofSourceSet("main").forEach { task ->
      task.plugins {
        id("grpc")
      }
      val copyTask = project.tasks.register<Copy>("${task.name}CopyToGenGrpc") {
        from(task.outputBaseDir)
        into("$projectDir/psi/gen-grpc/main")
      }
      task.finalizedBy(copyTask)
    }
  }
}

intellijPlatform {
  pluginConfiguration {
    name = providers.gradleProperty("pluginName")
    version = providers.gradleProperty("pluginVersion")

    // description handling skipped or simplified, assuming README doesn't have the exact same tags
    description = "IntelliJ Plugin for R language and RMarkdown support."

    changeNotes = providers.fileContents(layout.projectDirectory.file("Changes.md")).asText

    ideaVersion {
      sinceBuild = providers.gradleProperty("pluginSinceBuild")
    }
  }

  signing {
    certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
    privateKey = providers.environmentVariable("PRIVATE_KEY")
    password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
  }

  publishing {
    token = providers.environmentVariable("PUBLISH_TOKEN")
    channels =
      providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
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
  versionPrefix = ""
}

kover {
  reports {
    total {
      xml {
        onCheck = true
      }
    }
  }
}

tasks {
  wrapper {
    gradleVersion = providers.gradleProperty("gradleVersion").get()
  }

  publishPlugin {
    dependsOn(patchChangelog)
  }

  buildSearchableOptions {
    enabled = false
  }
}

intellijPlatformTesting {
  runIde {
    register("runIdeForUiTests") {
      task {
        jvmArgumentProviders += CommandLineArgumentProvider {
          listOf(
            "-Drobot-server.port=8082",
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false",
          )
        }
      }

      plugins {
        robotServerPlugin()
      }
    }
  }
}