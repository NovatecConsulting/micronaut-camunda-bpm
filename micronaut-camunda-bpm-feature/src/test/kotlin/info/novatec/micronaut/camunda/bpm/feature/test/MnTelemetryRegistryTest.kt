/*
 * Copyright 2020-2021 original authors
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

import info.novatec.micronaut.camunda.bpm.feature.MnTelemetryRegistry
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [MnTelemetryRegistry]
 *
 * @author Titus Meyer
 * @author Tobias Schäfer
 */
@MicronautTest
class MnTelemetryRegistryTest {

    @Inject
    lateinit var telemetryRegistry: MnTelemetryRegistry

    @Test
    fun `integration name is set to micronaut-camunda`() {
        assertEquals("micronaut-camunda", telemetryRegistry.camundaIntegration)
    }

    @Test
    fun `application server info is set to netty`() {
        val applicationServer = telemetryRegistry.applicationServer
        assertNotNull(applicationServer)
        assertEquals("netty", applicationServer.vendor)
        assertTrue(applicationServer.version.matches(Regex("""netty-\d+\.\d+\.\d+.*""")))
    }
}