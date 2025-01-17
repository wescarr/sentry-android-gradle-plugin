package io.sentry.android.gradle

import io.sentry.android.gradle.SentryPropertiesFileProvider.getPropertiesFilePath
import io.sentry.android.gradle.testutil.createTestAndroidProject
import java.io.File
import kotlin.test.assertEquals
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SentryPropertiesFileProviderTest {

    private val sep = File.separator

    @Test
    fun `getPropertiesFilePath finds file inside debug folder`() {
        val (project, android) = createTestAndroidProject()
        createTestFile(project.projectDir, "src${sep}debug${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside project folder`() {
        val (project, android) = createTestAndroidProject()
        createTestFile(project.projectDir, "sentry.properties")

        val variant = android.applicationVariants.first()

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside flavorName folder`() {
        val (project, android) = createTestAndroidProject {
            flavorDimensions("version")
            productFlavors.create("lite")
            productFlavors.create("full")
        }
        createTestFile(project.projectDir, "src${sep}lite${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside flavorName-buildType folder`() {
        val (project, android) = createTestAndroidProject {
            flavorDimensions("version")
            productFlavors.create("lite")
            productFlavors.create("full")
        }
        createTestFile(project.projectDir, "src${sep}lite${sep}debug${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside buildType-flavorName folder`() {
        val (project, android) = createTestAndroidProject {
            flavorDimensions("version")
            productFlavors.create("lite")
            productFlavors.create("full")
        }
        createTestFile(project.projectDir, "src${sep}debug${sep}lite${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath with multiple flavorDimensions finds file inside flavor folder`() {
        val (project, android) = createTestAndroidProject {
            flavorDimensions("version", "api")
            productFlavors.create("lite") {
                it.dimension("version")
            }
            productFlavors.create("api30") {
                it.dimension("api")
            }
        }
        createTestFile(project.projectDir, "src${sep}liteApi30${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteApi30Debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside other productFlavor folders`() {
        val (project, android) = createTestAndroidProject {
            flavorDimensions("version", "api")
            productFlavors.create("lite") {
                it.dimension("version")
            }
            productFlavors.create("api30") {
                it.dimension("api")
            }
        }
        createTestFile(project.projectDir, "src${sep}api30${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteApi30Debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root project folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val (project, android) = createTestAndroidProject(parent = rootProject)
        createTestFile(rootProject.projectDir, "sentry.properties")

        val variant = android.applicationVariants.first()

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root buildType folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val (project, android) = createTestAndroidProject(parent = rootProject)
        createTestFile(rootProject.projectDir, "src${sep}debug${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "debug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root flavor folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val (project, android) = createTestAndroidProject(parent = rootProject) {
            flavorDimensions("version")
            productFlavors.create("lite")
        }
        createTestFile(rootProject.projectDir, "src${sep}lite${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root flavor buildType folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val (project, android) = createTestAndroidProject(parent = rootProject) {
            flavorDimensions("version")
            productFlavors.create("lite")
        }
        createTestFile(rootProject.projectDir, "src${sep}lite${sep}debug${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    @Test
    fun `getPropertiesFilePath finds file inside root buildType flavor folder`() {
        val rootProject = ProjectBuilder.builder().build()
        val (project, android) = createTestAndroidProject(parent = rootProject) {
            flavorDimensions("version")
            productFlavors.create("lite")
        }
        createTestFile(rootProject.projectDir, "src${sep}debug${sep}lite${sep}sentry.properties")

        val variant = android.applicationVariants.first { it.name == "liteDebug" }

        assertEquals("42", File(getPropertiesFilePath(project, variant)!!).readText())
    }

    private fun createTestFile(parent: File, path: String) =
        File(parent, path).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("42")
        }
}
