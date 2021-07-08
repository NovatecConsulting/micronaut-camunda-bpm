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

import info.novatec.micronaut.camunda.bpm.feature.MnProcessEngineConfiguration
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * @author Martin Sawilla
 */
@MicronautTest
class ProcessEnginePluginsTest {

    @Inject
    lateinit var processEngineConfiguration: MnProcessEngineConfiguration

    @MockBean(ProcessEnginePlugin::class)
    fun myplugin(): ProcessEnginePlugin {
        return mock(ProcessEnginePlugin::class.java)
    }

    @Test
    fun `check that plugins are automatically loaded` () {
        val plugins = processEngineConfiguration.processEnginePlugins
        assertEquals(1, plugins.size)
    }
}
