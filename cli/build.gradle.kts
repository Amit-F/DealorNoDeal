
plugins { id("application") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies {
  implementation(project(":core"))
  implementation(project(":analytics"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}
application { mainClass.set("io.github.amitfink.deal.cli.Main") }
