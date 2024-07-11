buildscript {

  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }

  dependencies {
    classpath("com.android.tools.build:gradle:8.2.2")
    classpath("com.guardsquare:proguard-gradle:7.5.0")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
  }
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}
