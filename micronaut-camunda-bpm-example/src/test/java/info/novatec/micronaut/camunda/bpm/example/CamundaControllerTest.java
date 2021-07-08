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
package info.novatec.micronaut.camunda.bpm.example;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class CamundaControllerTest {

    @Inject
    @Client("/example")
    RxHttpClient client;

    @Test
    void name() {
        HttpRequest<String> request = HttpRequest.GET("/name");
        String body = client.toBlocking().retrieve(request);

        assertEquals("default", body);
    }

    @Test
    void definitions() {
        HttpRequest<String> request = HttpRequest.GET("/definitions");
        String body = client.toBlocking().retrieve(request);

        assertEquals("HelloWorld,Onboarding", body);
    }
}
