
plugins { id("java-library") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies {
  implementation(project(":core"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
  testImplementation("org.assertj:assertj-core:3.26.3")
}
