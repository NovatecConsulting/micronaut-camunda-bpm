/*
 * Copyright 2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.novatec.micronaut.camunda.bpm.feature.test

import info.novatec.micronaut.camunda.bpm.feature.LicenseKeyConfiguration
import io.micronaut.context.annotation.Property
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.camunda.bpm.engine.ManagementService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import java.util.*

/**
 * @author Martin Sawilla
 */
class LicenseKeyConfigurationTests {

    @MicronautTest
    @Nested
    inner class NoLicense {

        @Inject
        lateinit var managementService: ManagementService

        @Inject
        lateinit var licenseKeyConfiguration: Optional<LicenseKeyConfiguration>

        @BeforeEach
        fun `delete license` () {
            managementService.deleteLicenseKey()
        }

        @Test
        fun `no license provided`() {
            assertFalse(licenseKeyConfiguration.isPresent)
            assertNull(managementService.licenseKey)
        }
    }

    @MicronautTest
    @Nested
    @Property(name = "camunda.license-file", value = "")
    inner class LicenseFromResource {

        @Inject
        lateinit var managementService: ManagementService

        @Inject
        lateinit var licenseKeyConfiguration: Optional<LicenseKeyConfiguration>

        @BeforeEach
        fun `delete license` () {
            managementService.deleteLicenseKey()
        }

        @Test
        fun `load license from resource folder`() {
            assertTrue(licenseKeyConfiguration.isPresent)
            licenseKeyConfiguration.get().onApplicationEvent(ServerStartupEvent(Mockito.mock(EmbeddedServer::class.java)))

            val licenseKey = managementService.licenseKey
            assertNotNull(licenseKey)
            assertEquals("micronaut-camunda-bpm", getNameOfKey(licenseKey))
        }

        @Test
        fun `existing license is not overwritten`() {
            managementService.licenseKey = "test;limited"

            assertTrue(licenseKeyConfiguration.isPresent)
            licenseKeyConfiguration.get().onApplicationEvent(ServerStartupEvent(Mockito.mock(EmbeddedServer::class.java)))

            val licenseKey = managementService.licenseKey
            assertNotNull(licenseKey)
            assertEquals("limited", getNameOfKey(licenseKey))
        }

        @Test
        @Property(name = "camunda.license-file", value = " ")
        fun `license not loaded because url is blank`() {
            assertTrue(licenseKeyConfiguration.isPresent)
            licenseKeyConfiguration.get().onApplicationEvent(ServerStartupEvent(Mockito.mock(EmbeddedServer::class.java)))

            val licenseKey = managementService.licenseKey
            assertEquals(null, licenseKey)
        }

        @Test
        @Property(name = "camunda.license-file", value = "file:///does-not-exist.txt")
        fun `license not loaded because url points to a file that does not exist`() {
            assertTrue(licenseKeyConfiguration.isPresent)
            licenseKeyConfiguration.get().onApplicationEvent(ServerStartupEvent(Mockito.mock(EmbeddedServer::class.java)))

            val licenseKey = managementService.licenseKey
            assertEquals(null, licenseKey)
        }
    }

    @MicronautTest
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LicenseFromUrl: TestPropertyProvider {

        @Inject
        lateinit var managementService: ManagementService

        @Inject
        lateinit var licenseKeyConfiguration: Optional<LicenseKeyConfiguration>

        override fun getProperties(): MutableMap<String, String> {

            return mutableMapOf(
                "camunda.license-file" to LicenseFromUrl()::class.java.classLoader
                    .getResource("license/camunda-license-url.txt")!!
                    .toString()
                    .replace("file:/", "file:///"), // URI vs. URL ..
            )
        }

        @BeforeEach
        fun `delete license` () {
            managementService.deleteLicenseKey()
        }

        @Test
        fun `load license from an url` () {
            assertTrue(licenseKeyConfiguration.isPresent)
            licenseKeyConfiguration.get().onApplicationEvent(ServerStartupEvent(Mockito.mock(EmbeddedServer::class.java)))

            val licenseKey = managementService.licenseKey
            assertNotNull(licenseKey)
            assertEquals("micronaut-camunda-bpm-url", getNameOfKey(licenseKey))
        }
    }

    private fun getNameOfKey(key: String): String {
        return key.replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .split(";")[1]
    }
}
