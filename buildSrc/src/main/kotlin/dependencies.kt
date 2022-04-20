object versions {
    val java = org.gradle.api.JavaVersion.VERSION_17
    val jvm = "17"
    val kotlin = "1.6.10"
    val kotlin_coroutines = "1.6.0"
    val kotlin_serialization_json = "1.3.2"
    val kotest = "5.2.3"
    val jooq = "3.16.5"
    val flywaydb = "8.5.4"
    val opentracing = "0.33.0"
    val jaeger = "1.8.0"
    val micrometer = "1.8.4"
    val detekt = "1.18.1"
    val shadow_jar_plugin = "7.0.0"
    val logback_json = "0.1.5"
    val kotlin_logging = "2.1.21"
    val jackson = "2.13.2"
    val mockk = "1.12.3"
    val testcontainers = "1.16.3"
    val postgresql = "42.3.3"
    val vertx = "4.2.6"
    val hikari = "5.0.1"
    val kafka_clients = "3.1.0"
}

object libs {
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect"
    val kotlin_serialization = "org.jetbrains.kotlin:kotlin-serialization:${versions.kotlin}"
    val kotlin_logging = "io.github.microutils:kotlin-logging:${versions.kotlin_logging}"
    val kotlinx_coroutines_bom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:${versions.kotlin_coroutines}"
    val kotlin_bom = "org.jetbrains.kotlin:kotlin-bom:${versions.kotlin}"
    val kotlin_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.kotlin_coroutines}"
    val kotlinx_serialization = "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm"
    val kotlinx_serialization_json = "org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.kotlin_serialization_json}"
    val kotest_runner = "io.kotest:kotest-runner-junit5:${versions.kotest}"
    val kotest_assertions_json = "io.kotest:kotest-assertions-json-jvm:${versions.kotest}"
    val kotest_assertions_core = "io.kotest:kotest-assertions-core-jvm:${versions.kotest}"
    val kotest_framework = "io.kotest:kotest-framework-api:${versions.kotest}"
    val hikari = "com.zaxxer:HikariCP:${versions.hikari}"
    val jooq = "org.jooq:jooq:${versions.jooq}"
    val jackson_bom = "com.fasterxml.jackson:jackson-bom:${versions.jackson}"
    val jaeger_client = "io.jaegertracing:jaeger-client:${versions.jaeger}"
    val jaeger_micrometer = "io.jaegertracing:jaeger-micrometer:${versions.jaeger}"
    val logback_jackson = "ch.qos.logback.contrib:logback-jackson:${versions.logback_json}"
    val micrometer = "io.micrometer:micrometer-core:${versions.micrometer}"
    val opentracing_api = "io.opentracing:opentracing-api:${versions.opentracing}"
    val flyway = "org.flywaydb:flyway-core:${versions.flywaydb}"
    val postgres = "org.postgresql:postgresql:${versions.postgresql}"
    val vertx_pg = "io.vertx:vertx-pg-client:${versions.vertx}"
    val vertx_kotlin = "io.vertx:vertx-lang-kotlin:${versions.vertx}"
    val vertx_kotlin_coroutines = "io.vertx:vertx-lang-kotlin-coroutines:${versions.vertx}"
    val mockk = "io.mockk:mockk:${versions.mockk}"
    val kafka_clients = "org.apache.kafka:kafka-clients:${versions.kafka_clients}"
    val testcontainers_bom = "org.testcontainers:testcontainers-bom:${versions.testcontainers}"
    val testcontainers = "org.testcontainers:testcontainers"
    val testcontainers_postgres = "org.testcontainers:postgresql"
    val detekt = "io.gitlab.arturbosch.detekt:detekt-formatting:${versions.detekt}"
    val micrometer_prometheus = "io.micrometer:micrometer-registry-prometheus:${versions.micrometer}"

    val detekt_plugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${versions.detekt}"
    val shadow_jar_plugin = "gradle.plugin.com.github.jengelman.gradle.plugins:shadow:${versions.shadow_jar_plugin}"
    val kotlin_plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
}
