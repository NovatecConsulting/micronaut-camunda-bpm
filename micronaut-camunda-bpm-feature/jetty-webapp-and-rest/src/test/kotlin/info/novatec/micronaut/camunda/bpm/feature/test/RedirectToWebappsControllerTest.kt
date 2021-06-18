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

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedirectToWebappsControllerTest : TestPropertyProvider {

    override fun getProperties(): MutableMap<String, String> {
        return mutableMapOf(
            "camunda.webapps.enabled" to "false",
            "camunda.webapps.index-redirect-enabled" to "true",
        )
    }

    @Inject
    @field:Client("/")
    lateinit var client: RxHttpClient

    @Test
    fun `redirect is disabled because webapps are disabled`() {
        val request: HttpRequest<String> = HttpRequest.GET("/")
        assertThrows(HttpClientResponseException::class.java) {
            client.toBlocking().exchange<String, Any>(request)
        }
    }
}