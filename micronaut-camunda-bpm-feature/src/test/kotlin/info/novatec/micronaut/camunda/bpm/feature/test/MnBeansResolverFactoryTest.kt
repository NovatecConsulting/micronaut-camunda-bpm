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

import info.novatec.micronaut.camunda.bpm.feature.test.ProcessUtil.Companion.deploy
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.ScriptEvaluationException
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Tests for [info.novatec.micronaut.camunda.bpm.feature.MnBeansResolverFactory]
 *
 * @author Titus Meyer
 */
@MicronautTest
class MnBeansResolverFactoryTest {
    @Inject
    lateinit var runtimeService: RuntimeService

    @Inject
    lateinit var repositoryService: RepositoryService

    @Inject
    lateinit var micronautBean: MicronautBean

    @Inject
    lateinit var myNamedBean: MicronautNamedBean

    @Test
    fun `service task with groovy script and bean`() {
        val processId = "processWithScriptTaskAndBean"
        deploy(
            repositoryService,
            Bpmn.createProcess(processId)
                .executable()
                .startEvent()
                .scriptTask().scriptFormat("groovy").scriptText(
                    """
                        micronautBean.setSum(0)
                        for (int i in 1..5) {
                          int newValue = micronautBean.getSum() + i
                          micronautBean.setSum(newValue)
                        }
                    """.trimIndent()
                )
                .endEvent()
        )
        runtimeService.startProcessInstanceByKey(processId)

        assertEquals(15, micronautBean.getSum())
    }

    @Test
    fun `service task with groovy script and named bean`() {
        val processId = "processWithScriptTaskAndNamedBean"
        deploy(
            repositoryService,
            Bpmn.createProcess(processId)
                .executable()
                .startEvent()
                .scriptTask().scriptFormat("groovy").scriptText(
                    """
                        myNamedBean.setSum(0)
                        for (int i in 1..5) {
                          int newValue = myNamedBean.getSum() + i
                          myNamedBean.setSum(newValue)
                        }
                    """.trimIndent()
                )
                .endEvent()
        )
        runtimeService.startProcessInstanceByKey(processId)

        assertEquals(15, myNamedBean.getSum())
    }

    @Test
    fun `service task with groovy script and unknown bean`() {
        val processId = "processWithScriptTaskAndUnknownBean"
        deploy(
            repositoryService,
            Bpmn.createProcess(processId)
                .executable()
                .startEvent()
                .scriptTask().scriptFormat("groovy").scriptText("unknownBean.doSomething()")
                .endEvent()
        )
        assertThrows(ScriptEvaluationException::class.java) { runtimeService.startProcessInstanceByKey(processId) }
    }

    @Singleton
    open class MicronautBean {
        private var sum: Int = 0

        fun getSum(): Int {
            return sum
        }

        fun setSum(sum: Int) {
            this.sum = sum
        }

        fun reset() {
            sum = 0
        }
    }

    @Singleton
    @Named("myNamedBean")
    class MicronautNamedBean : MicronautBean()
}