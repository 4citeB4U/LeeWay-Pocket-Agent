/* REGION: LEEWAY.BRAIN.BUILD; TAG: SHARED_CORE_JVM_QUALIFICATION
WHO: LeeWay; WHAT: Build the OS-neutral Brain core; WHY: Native APIs belong in adapters.
WHEN: Build; WHERE: shared module; HOW: Kotlin/JVM, no Android plugin or model dependency.
LICENSE: MIT */
plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
dependencies { testImplementation("junit:junit:4.13.2") }