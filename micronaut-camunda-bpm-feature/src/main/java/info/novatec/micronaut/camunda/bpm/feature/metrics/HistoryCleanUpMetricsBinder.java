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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import org.camunda.bpm.engine.management.Metrics;

import javax.inject.Singleton;

import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.MICRONAUT_METRICS_BINDERS_CAMUNDA_HISTORY_CLEAN_UP_ENABLED;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.HISTORY_CLEAN_UP_DEFAULT_TAGS;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.TAG_KEY_NAME;
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Provides Metrics about History Cleanup Execution.
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS_CAMUNDA_HISTORY_CLEAN_UP_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Requires(property = CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED, notEquals = StringUtils.FALSE)
public class HistoryCleanUpMetricsBinder implements MeterBinder {

    protected final HistoryCleanUpMetrics historyCleanUpMetrics;

    public HistoryCleanUpMetricsBinder(HistoryCleanUpMetrics historyCleanUpMetrics) {
        this.historyCleanUpMetrics = historyCleanUpMetrics;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {

        Gauge.builder(
            createMetricName(Metrics.HISTORY_CLEANUP_REMOVED_PROCESS_INSTANCES),
            historyCleanUpMetrics,
            HistoryCleanUpMetrics::getRemovedProcessInstances
        )
            .description("The number of process instances removed by history clean up.")
            .tags(Tags.concat(HISTORY_CLEAN_UP_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.HISTORY_CLEANUP_REMOVED_PROCESS_INSTANCES))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.HISTORY_CLEANUP_REMOVED_DECISION_INSTANCES),
            historyCleanUpMetrics,
            HistoryCleanUpMetrics::getRemovedDecisionInstances
        )
            .description("The number of decision instances removed by history clean up.")
            .tags(Tags.concat(HISTORY_CLEAN_UP_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.HISTORY_CLEANUP_REMOVED_DECISION_INSTANCES))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.HISTORY_CLEANUP_REMOVED_BATCH_OPERATIONS),
            historyCleanUpMetrics,
            HistoryCleanUpMetrics::getRemovedBatchOperations
        )
            .description("The number of batch operations removed by history clean up.")
            .tags(Tags.concat(HISTORY_CLEAN_UP_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.HISTORY_CLEANUP_REMOVED_BATCH_OPERATIONS))
            .register(registry);
    }

    private String createMetricName(String camundaName) {
        return "camunda." + camundaName.replace('-', '.');
    }

}
