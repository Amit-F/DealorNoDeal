
plugins { id("java-library") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies {
  // Align all JUnit components to the same version
  testImplementation(platform("org.junit:junit-bom:5.11.0"))

  // JUnit Jupiter API +Engine (pulled via the BOM)
  testImplementation("org.junit.jupiter:junit-jupiter")

  // Required at runtime to launch the platform under Gradle 9
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  // Assertions
  testImplementation("org.assertj:assertj-core:3.26.3")
}

