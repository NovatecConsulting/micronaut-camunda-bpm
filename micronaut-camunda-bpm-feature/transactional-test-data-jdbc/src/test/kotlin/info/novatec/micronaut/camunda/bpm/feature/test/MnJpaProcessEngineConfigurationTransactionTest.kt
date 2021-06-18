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
import jakarta.inject.Singleton
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Executes the inherited tests with a JPA configuration. [PersistBookDelegate] replaces [DoNothingDelegate] and persists entities.
 *
 * @author Lukasz Frankowski
 * @author Tobias Schäfer
 */
@MicronautTest(transactional = false)
class MnJpaProcessEngineConfigurationTransactionTest : MnProcessEngineConfigurationTransactionTest() {

    @Inject
    lateinit var bookRepository: BookRepository

    @BeforeEach
    override fun deployProcessModel() {
        ProcessUtil.deploy(repositoryService, Bpmn.createProcess("SaveBookProcess")
                .executable()
                .startEvent()
                .serviceTask().camundaDelegateExpression("\${saveBookDelegate}")
                .endEvent())
    }

    @Test
    override fun testCommit() {
        super.testCommit()
        assertEquals(1, bookRepository.countByTitle(WITH_COMMIT))
    }

    @Test
    override fun testRollback() {
        super.testRollback()
        assertEquals(0, bookRepository.countByTitle(WITH_ROLLBACK))
    }

    @Test
    override fun testSurroundingTransactionWithCommit() {
        super.testSurroundingTransactionWithCommit()
        assertEquals(2, bookRepository.countByTitle(TX_WITH_COMMIT))
    }

    @Test
    override fun testNoSurroundingTransactionWithCommits() {
        super.testNoSurroundingTransactionWithCommits()
        assertEquals(2, bookRepository.countByTitle(WITH_COMMIT_COMMIT))
    }

    @Test
    override fun testNoSurroundingTransactionWithCommitAndRollback() {
        super.testNoSurroundingTransactionWithCommitAndRollback()
        assertEquals(1, bookRepository.countByTitle(WITH_COMMIT_ROLLBACK))
    }

    @Test
    override fun testSurroundingTransactionWithRollback() {
        super.testSurroundingTransactionWithRollback()
        assertEquals(0, bookRepository.countByTitle(TX_WITH_ROLLBACK))
    }

    override fun startProcess(businessKey: String): String {
        return runtimeService.startProcessInstanceByKey("SaveBookProcess", businessKey).id
    }

    @Singleton
    class SaveBookDelegate : JavaDelegate {
        @Inject
        lateinit var bookRepository: BookRepository
        override fun execute(execution: DelegateExecution) {
            bookRepository.save(Book(execution.businessKey))
        }
    }
}