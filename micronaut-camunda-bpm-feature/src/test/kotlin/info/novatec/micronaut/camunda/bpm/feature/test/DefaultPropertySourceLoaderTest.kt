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

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Test that [info.novatec.micronaut.camunda.bpm.feature.DefaultPropertySourceLoader] handles the custom properties correctly.
 *
 * @author Tobias Schäfer
 */
class DefaultPropertySourceLoaderTest {

    @MicronautTest
    @Nested
    internal inner class DefaultPropertyValue {
        @Inject
        lateinit var applicationContext: ApplicationContext

        @Test
        fun `default value`() {
            assertEquals(50, applicationContext.environment.getProperty("datasources.default.maximum-pool-size", Int::class.java).get())
        }
    }

    @MicronautTest
    @Nested
    @Property(name = "datasources.default.maximumPoolSize", value = "42")
    internal inner class OverwrittenPropertyValue {

        @Inject
        lateinit var applicationContext: ApplicationContext

        @Test
        fun `property can be overwritten`() {
            assertEquals(42, applicationContext.environment.getProperty("datasources.default.maximum-pool-size", Int::class.java).get())
        }
    }
}
