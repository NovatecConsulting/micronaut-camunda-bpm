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

import info.novatec.micronaut.camunda.bpm.feature.MnProcessEngineConfiguration
import info.novatec.micronaut.camunda.bpm.feature.ProcessEngineConfigurationCustomizer
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton

@Singleton
@Replaces(ProcessEngineConfigurationCustomizer::class)
class MicronautProcessEngineConfigurationCustomizer : ProcessEngineConfigurationCustomizer {
    override fun customize(processEngineConfiguration: MnProcessEngineConfiguration) {
        processEngineConfiguration.processEngineName = PROCESS_ENGINE_NAME
    }

    companion object {
        const val PROCESS_ENGINE_NAME = "CustomizedEngine"
    }
}