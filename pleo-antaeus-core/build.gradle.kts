plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
    implementation("io.javalin:javalin:4.6.4")
    implementation("org.javamoney:moneta:1.4.2")

}