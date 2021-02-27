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

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.camunda.bpm.engine.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.inject.Inject

@MicronautTest
class CamundaServicesFactoryTest {
    @Inject
    lateinit var runtimeService: RuntimeService

    @Inject
    lateinit var repositoryService: RepositoryService

    @Inject
    lateinit var managementService: ManagementService

    @Inject
    lateinit var authorizationService: AuthorizationService

    @Inject
    lateinit var decisionService: DecisionService

    @Inject
    lateinit var externalTaskService: ExternalTaskService

    @Inject
    lateinit var filterService: FilterService

    @Inject
    lateinit var formService: FormService

    @Inject
    lateinit var taskService: TaskService

    @Inject
    lateinit var historyService: HistoryService

    @Inject
    lateinit var identityService: IdentityService

    @Test
    fun `all service beans are available in applicationContext`() {
        assertNotNull(runtimeService)
        assertNotNull(repositoryService)
        assertNotNull(managementService)
        assertNotNull(authorizationService)
        assertNotNull(decisionService)
        assertNotNull(externalTaskService)
        assertNotNull(filterService)
        assertNotNull(formService)
        assertNotNull(taskService)
        assertNotNull(historyService)
        assertNotNull(identityService)
    }
}