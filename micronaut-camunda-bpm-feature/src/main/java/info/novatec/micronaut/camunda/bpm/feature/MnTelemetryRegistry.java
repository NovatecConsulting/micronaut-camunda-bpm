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
package info.novatec.micronaut.camunda.bpm.feature;

import jakarta.inject.Singleton;
import org.camunda.bpm.engine.impl.telemetry.TelemetryRegistry;
import org.camunda.bpm.engine.impl.telemetry.dto.ApplicationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Micronaut specific implementation of {@link TelemetryRegistry}.
 *
 * @author Tobias Schäfer
 * @author Titus Meyer
 */
@Singleton
public class MnTelemetryRegistry extends TelemetryRegistry {
    private static final Logger log = LoggerFactory.getLogger(MnTelemetryRegistry.class);

    protected static final String INTEGRATION_NAME = "micronaut-camunda";

    public MnTelemetryRegistry(Optional<ApplicationServer> applicationServer) {
        setCamundaIntegration(INTEGRATION_NAME);
        if (applicationServer.isPresent()) {
            log.info("Server runtime version: vendor={}, version={}", applicationServer.get().getVendor(), applicationServer.get().getVersion());
            setApplicationServer(applicationServer.get());
        } else {
            log.warn("Unable to identify the application server for the telemetry data!");
        }
    }
}