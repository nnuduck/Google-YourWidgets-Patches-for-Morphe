group = "com.github.nnuduck.yourwidget"

patches {
    extensionsProjectPath = null

    about {
        name = "Google YourWidgets Patches for Morphe"
        description = "Third-party compatibility and GmsCore redirect patches for Google Create My Widget, compatible with Morphe."
        source = "git@github.com:nnuduck/Google-YourWidgets-Patches-for-Morphe.git"
        author = "nnuduck"
        contact = "https://github.com/nnuduck/Google-YourWidgets-Patches-for-Morphe/issues"
        website = "https://github.com/nnuduck/Google-YourWidgets-Patches-for-Morphe"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    implementation(libs.morphe.patches.library) {
        exclude(group = "app.morphe", module = "morphe-patcher")
        exclude(group = "com.github.MorpheApp.smali")
    }
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Generates patches-list.json from the compiled .mpp patch bundle."
        group = "build"

        dependsOn("buildAndroid")
        workingDir = projectDir
        classpath = configurations.runtimeClasspath.get() + files(layout.buildDirectory.dir("libs"))
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }
}
