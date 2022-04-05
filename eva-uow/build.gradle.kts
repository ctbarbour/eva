plugins {
    id("eva-kotlin")
}

kotlin.target.compilations.getByName("testFixtures") {
    associateWith(kotlin.target.compilations.getByName("main"))
}

dependencies {
    implementation(libs.kotlin_stdlib)
    implementation(libs.kotlin_reflect)
    implementation(libs.kotlin_coroutines)
    implementation(libs.kotlin_logging)

    implementation(project(eva.eva_idempotency_key))
    implementation(project(eva.eva_domain))
    implementation(project(eva.eva_repository))
    implementation(project(eva.eva_persistence))
    implementation(project(eva.eva_events))
    implementation(project(eva.eva_tracing))

    testImplementation(project(eva.eva_migrations))
    testImplementation(project(eva.eva_persistence))
    testImplementation(project(eva.eva_persistence_jdbc))
    testImplementation(project(eva.eva_persistence_vertx))
    testImplementation(project(eva.eva_test))
    testImplementation(project(eva.eva_serialization))
    testImplementation(testFixtures(project(eva.eva_domain)))
    testImplementation(testFixtures(project(eva.eva_repository)))
    testImplementation(testFixtures(project(eva.eva_persistence)))

    testFixturesImplementation(libs.kotest_framework)
    testFixturesImplementation(libs.kotlin_coroutines)
    testFixturesImplementation(project(eva.eva_repository))
    testFixturesImplementation(project(eva.eva_test))
    testFixturesImplementation(testFixtures(project(eva.eva_repository)))
    testFixturesImplementation(testFixtures(project(eva.eva_domain)))
    testFixturesImplementation(testFixtures(project(eva.eva_persistence)))
}
