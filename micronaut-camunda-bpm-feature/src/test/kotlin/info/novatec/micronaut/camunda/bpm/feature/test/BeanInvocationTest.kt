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
import jakarta.inject.Inject
import jakarta.inject.Named
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

@MicronautTest
class BeanInvocationTest {
    @Inject
    lateinit var runtimeService: RuntimeService

    @Inject
    lateinit var repositoryService: RepositoryService

    @Inject
    @Named("myDelegate")
    lateinit var myDelegate: JavaDelegate

    @BeforeEach
    fun init() {
        reset(myDelegate)
    }

    @Test
    fun `service task with expression delegate`() {
        val processId = "processWithExpressionDelegate"
        ProcessUtil.deploy(repositoryService, Bpmn.createProcess(processId)
                .executable()
                .startEvent()
                .serviceTask().camundaDelegateExpression("\${myDelegate}")
                .endEvent())
        runtimeService.startProcessInstanceByKey(processId)
        verify(myDelegate).execute(ArgumentMatchers.any(DelegateExecution::class.java))
    }

    @Test
    fun `service task with Java class name`() {
        val processId = "processWithJavaClassName"
        ProcessUtil.deploy(repositoryService, Bpmn.createProcess(processId)
                .executable()
                .startEvent()
                .serviceTask().camundaClass(JavaDelegate::class.java.name)
                .endEvent())
        runtimeService.startProcessInstanceByKey(processId)
        verify(myDelegate).execute(ArgumentMatchers.any(DelegateExecution::class.java))
    }

    @Test
    fun `service task with unmanaged Java delegate`() {
        val processId = "unmanagedJavaDelegate"
        ProcessUtil.deploy(repositoryService, Bpmn.createProcess(processId)
                .executable()
                .startEvent()
                .serviceTask().camundaClass(UnmanagedJavaDelegate::class.java.name)
                .endEvent())
        runtimeService.startProcessInstanceByKey(processId)
    }

    class UnmanagedJavaDelegate : JavaDelegate {
        override fun execute(execution: DelegateExecution) {}
    }
}