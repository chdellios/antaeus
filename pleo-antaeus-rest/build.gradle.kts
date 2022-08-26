plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")
    implementation("io.javalin:javalin:4.6.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
    implementation("org.slf4j:slf4j-simple:1.7.26")

}
