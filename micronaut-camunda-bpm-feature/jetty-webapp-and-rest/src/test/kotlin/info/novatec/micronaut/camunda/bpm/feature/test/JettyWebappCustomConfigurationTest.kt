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

import info.novatec.micronaut.camunda.bpm.feature.Configuration
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.eclipse.jetty.server.Server
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test the customized Webapps on Jetty.
 *
 * @author Martin Sawilla
 */
@MicronautTest
@Requires(beans = [Server::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JettyWebappCustomConfigurationTest : TestPropertyProvider {

    override fun getProperties(): MutableMap<String, String> {
        return mutableMapOf(
            "camunda.webapps.context-path" to "/custom-webapp-path",
            "camunda.webapps.index-redirect-enabled" to "false",
            "camunda.rest.context-path" to "/custom-rest-path",
        )
    }

    @Inject
    lateinit var configuration: Configuration

    @Inject
    @field:Client("/")
    lateinit var client: RxHttpClient

    @Test
    fun `redirect is disabled by application configuration`() {
        val request: HttpRequest<String> = HttpRequest.GET("/")
        assertThrows(HttpClientResponseException::class.java) {
            client.toBlocking().exchange<String, Any>(request)
        }
    }

    @Test
    fun `welcome app is available` () {
        val request: HttpRequest<String> = HttpRequest.GET(configuration.webapps.contextPath + "/app/welcome/default")
        val res: HttpResponse<*> = client.toBlocking().exchange<String, Any>(request)
        assertEquals(HttpStatus.OK, res.status())
    }

    @Test
    fun `admin app is available` () {
        val request: HttpRequest<String> = HttpRequest.GET(configuration.webapps.contextPath + "/app/admin/default")
        val res: HttpResponse<*> = client.toBlocking().exchange<String, Any>(request)
        assertEquals(HttpStatus.OK, res.status())
    }

    @Test
    fun `cockpit app is available` () {
        val request: HttpRequest<String> = HttpRequest.GET(configuration.webapps.contextPath + "/app/cockpit/default")
        val res: HttpResponse<*> = client.toBlocking().exchange<String, Any>(request)
        assertEquals(HttpStatus.OK, res.status())
    }

    @Test
    fun `tasklist app is available` () {
        val request: HttpRequest<String> = HttpRequest.GET(configuration.webapps.contextPath + "/app/tasklist/default")
        val res: HttpResponse<*> = client.toBlocking().exchange<String, Any>(request)
        assertEquals(HttpStatus.OK, res.status())
    }
}
