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

import io.micronaut.context.annotation.Replaces
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import javax.inject.Named
import javax.inject.Singleton

/**
 * [JavaDelegate] which replaces @[RuntimeErrorDelegate] which first saves a new entity and then throws a [RuntimeException].
 *
 * In a transactional context the persistence of the entity should be rolled back.
 *
 * @author Tobias Schäfer
 */
@Singleton
@Replaces(RuntimeErrorDelegate::class)
@Named("runtimeErrorDelegate")
class RuntimeErrorDelegateJpa(private val bookRepository: BookRepository) : JavaDelegate {
    override fun execute(execution: DelegateExecution) {
        bookRepository.save(Book(execution.businessKey))
        throw RuntimeException("RuntimeErrorDelegate throws RuntimeException")
    }
}