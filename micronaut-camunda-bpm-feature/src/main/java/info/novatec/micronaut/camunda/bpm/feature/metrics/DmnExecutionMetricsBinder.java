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
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.MICRONAUT_METRICS_BINDERS_CAMUNDA_DMN_EXECUTION_ENABLED;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.DMN_EXECUTION_DEFAULT_TAGS;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.TAG_KEY_NAME;
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Provides Metrics about DMN Execution.
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS_CAMUNDA_DMN_EXECUTION_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Requires(property = CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED, notEquals = StringUtils.FALSE)
public class DmnExecutionMetricsBinder implements MeterBinder {

    protected final DmnExecutionMetrics dmnExecutionMetrics;

    public DmnExecutionMetricsBinder(DmnExecutionMetrics dmnExecutionMetrics) {
        this.dmnExecutionMetrics = dmnExecutionMetrics;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {

        Gauge.builder(
            createMetricName(Metrics.EXECUTED_DECISION_INSTANCES),
            dmnExecutionMetrics,
            DmnExecutionMetrics::countExecutedDecisionInstances
        )
            .description("The number of evaluated decision instances (EDI). A decision instance is a DMN decision table or a DMN Literal Expression.")
            .tags(Tags.concat(DMN_EXECUTION_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.EXECUTED_DECISION_INSTANCES))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.EXECUTED_DECISION_ELEMENTS),
            dmnExecutionMetrics,
            DmnExecutionMetrics::countExecutedDecisionElements
        )
            .description("The number of decision elements executed during evaluation of DMN decision tables. For one table, this is calculated as the number of clauses multiplied by the number of rules.")
            .tags(Tags.concat(DMN_EXECUTION_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.EXECUTED_DECISION_ELEMENTS))
            .register(registry);
    }

    private String createMetricName(String camundaName) {
        return "camunda.dmn." + camundaName.replace('-', '.');
    }

}
