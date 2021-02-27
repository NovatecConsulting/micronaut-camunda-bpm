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
package info.novatec.micronaut.camunda.bpm.feature;

import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.order.Ordered;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link io.micronaut.context.env.PropertySourceLoader} which sets some default values
 * with lowest precedence.
 *
 * @author Tobias Schäfer
 */
public class DefaultPropertySourceLoader implements PropertySourceLoader, Ordered {

    /**
     * Position for the system property source loader in the chain.
     */
    protected static final int POSITION = Ordered.LOWEST_PRECEDENCE;

    /** The (Hikari) datasource pool size must be larger than the number of competing threads
     * so that they don't get blocked while waiting for a connection and smaller than the maximum parallel
     * connections supported by the database, e.g. 100 on PostgreSQL.
     */
    protected static final int MAXIMUM_POOL_SIZE = 50;

    /**
     * By default, the (Hikari) minimum-idle value is the same as the maximumPoolSize, see https://github.com/brettwooldridge/HikariCP
     * However, multiple applications would not work with this default. Therefore we reduce the minimum-idle configuration.
     */
    protected static final int MINIMUM_POOL_SIZE = 10;

    @Override
    public int getOrder() {
        return POSITION;
    }

    @Override
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader) {
        return Optional.of(
                PropertySource.of(
                        "micronaut-camunda-bpm-defaults",
                        new HashMap<String, Object>() {{
                            put("datasources.default.maximum-pool-size", MAXIMUM_POOL_SIZE);
                            put("datasources.default.minimum-idle", MINIMUM_POOL_SIZE);
                            put("camunda.generic-properties.properties.cmmn-enabled", false);
                        }}));
    }

    @Override
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader, ActiveEnvironment activeEnvironment) {
        return Optional.empty();
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) {
        return new HashMap<>();
    }
}
