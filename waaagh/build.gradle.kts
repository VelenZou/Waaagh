plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.waaagh"
// CI can override via -PpluginVersion=1.2.3 (e.g. from a v* git tag)
version = (findProperty("pluginVersion") as String?) ?: "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 目标平台：统一版 IntelliJ IDEA（2025.3 起 Community/Ultimate 合并）
        intellijIdea("2026.2.3")
        bundledPlugin("com.intellij.java")
    }

    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("org.yaml:snakeyaml:1.29")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-collections:commons-collections:3.2.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 起始支持版本，2020.3 (IDEA 201)
            sinceBuild = "203"
            // 不设兼容上限
            untilBuild = provider { null }
        }
    }

    signing {
        // Signing is optional for Marketplace publishing. Only enable it when real credentials are
        // provided; otherwise leave the properties unset so signPlugin is skipped and publishPlugin
        // uploads the unsigned zip. (In CI, unset secrets arrive as EMPTY strings — setting them
        // would make signPlugin run and NPE on a blank private key.)
        val certificateChainEnv = System.getenv("CERTIFICATE_CHAIN")
        val privateKeyEnv = System.getenv("PRIVATE_KEY")
        if (!certificateChainEnv.isNullOrBlank() && !privateKeyEnv.isNullOrBlank()) {
            certificateChain = certificateChainEnv
            privateKey = privateKeyEnv
            password = System.getenv("PRIVATE_KEY_PASSWORD")
        }
    }

    // publishing token 默认取 PUBLISH_TOKEN 环境变量，无需显式配置
}

tasks {
    // 保持 Java 11 字节码，兼容旧版本 IDE 的运行时
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}
