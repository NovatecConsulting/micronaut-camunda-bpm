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
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.MICRONAUT_METRICS_BINDERS_CAMUNDA_HISTORY_CLEAN_UP_ENABLED;
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Holder class to count history cleanup execution metrics.
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS_CAMUNDA_HISTORY_CLEAN_UP_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Requires(property = CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED, notEquals = StringUtils.FALSE)
public class HistoryCleanUpMetrics extends CamundaMetrics {

    public HistoryCleanUpMetrics(BeanProvider<ManagementService> providerManagementService) {
        super(providerManagementService);
    }

    public long getRemovedProcessInstances() {
        return sumMetric(Metrics.HISTORY_CLEANUP_REMOVED_PROCESS_INSTANCES);
    }

    public long getRemovedDecisionInstances() {
        return sumMetric(Metrics.HISTORY_CLEANUP_REMOVED_DECISION_INSTANCES);
    }

    public long getRemovedBatchOperations() {
        return sumMetric(Metrics.HISTORY_CLEANUP_REMOVED_BATCH_OPERATIONS);
    }

}
