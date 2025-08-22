
plugins { id("application") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
application { mainClass.set("io.github.amitfink.deal.legacy.LegacyPlaceholder") }
