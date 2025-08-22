
plugins { id("application") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies {
  implementation(project(":core"))
  implementation(project(":analytics"))

  testImplementation(platform("org.junit:junit-bom:5.11.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  testImplementation("org.assertj:assertj-core:3.26.3")
}
application { mainClass.set("io.github.amitfink.deal.cli.Main") }
