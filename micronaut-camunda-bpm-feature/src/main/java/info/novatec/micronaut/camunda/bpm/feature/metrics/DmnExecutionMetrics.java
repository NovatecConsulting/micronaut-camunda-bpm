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
package info.novatec.micronaut.camunda.bpm.feature.metrics;

import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.management.Metrics;

import javax.inject.Singleton;

import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.MICRONAUT_METRICS_BINDERS_CAMUNDA_DMN_EXECUTION_ENABLED;
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Holder class to count dmn execution metrics.
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS_CAMUNDA_DMN_EXECUTION_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Requires(property = CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED, notEquals = StringUtils.FALSE)
public class DmnExecutionMetrics extends CamundaMetrics {

    public DmnExecutionMetrics(BeanProvider<ManagementService> providerManagementService) {
        super(providerManagementService);
    }

    public long countExecutedDecisionInstances() {
        return sumMetric(Metrics.EXECUTED_DECISION_INSTANCES);
    }

    public long countExecutedDecisionElements() {
        return sumMetric(Metrics.EXECUTED_DECISION_ELEMENTS);
    }

}
