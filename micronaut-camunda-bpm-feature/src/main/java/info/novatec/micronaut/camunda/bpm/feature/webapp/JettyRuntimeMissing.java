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
package info.novatec.micronaut.camunda.bpm.feature.webapp;

import info.novatec.micronaut.camunda.bpm.feature.Configuration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.micronaut.servlet.jetty.JettyServer;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log a warning if Webapps or REST are enabled but not supported on the current server runtime.
 *
 * Currently, the Webapps and REST are only supported on Jetty.
 *
 * @author Tobias Schäfer
 */
@Singleton
@Requires(missing = JettyServer.class)
public class JettyRuntimeMissing implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger log = LoggerFactory.getLogger(JettyRuntimeMissing.class);

    protected final Configuration configuration;

    public JettyRuntimeMissing(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        if (configuration.getWebapps().isEnabled()) {
            log.warn("Webapps are enabled via 'camunda.webapps.enabled' but they are not supported on the current server runtime. Please switch to Jetty, see https://github.com/camunda-community-hub/micronaut-camunda-bpm#camunda-rest-api-and-webapps");
        }
        if (configuration.getRest().isEnabled()) {
            log.warn("REST are enabled via 'camunda.rest.enabled' but it is not supported on the current server runtime. Please switch to Jetty, see https://github.com/camunda-community-hub/micronaut-camunda-bpm#camunda-rest-api-and-webapps");
        }
    }
}
