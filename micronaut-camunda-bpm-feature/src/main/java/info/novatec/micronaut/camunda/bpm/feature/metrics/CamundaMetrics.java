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

import io.micronaut.context.BeanProvider;
import io.micronaut.context.exceptions.BeanInstantiationException;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.management.MetricsQuery;

import java.util.HashMap;
import java.util.Map;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Base class to use inbuild camunda metrics.
 * see also https://docs.camunda.org/manual/latest/user-guide/process-engine/metrics/
 */
abstract public class CamundaMetrics {

    static final String CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED = "camunda.generic-properties.properties.metricsEnabled";

    static final String MICRONAUT_METRICS_BINDERS_CAMUNDA_BPMN_EXECUTION_ENABLED = MICRONAUT_METRICS_BINDERS + ".camunda.bpmnExecution.enabled";

    static final String MICRONAUT_METRICS_BINDERS_CAMUNDA_DMN_EXECUTION_ENABLED = MICRONAUT_METRICS_BINDERS + ".camunda.dmnExecution.enabled";

    static final String MICRONAUT_METRICS_BINDERS_CAMUNDA_HISTORY_CLEAN_UP_ENABLED = MICRONAUT_METRICS_BINDERS + ".camunda.historyCleanUp.enabled";

    static final String MICRONAUT_METRICS_BINDERS_CAMUNDA_JOB_EXECUTOR_ENABLED = MICRONAUT_METRICS_BINDERS + ".camunda.jobExecutor.enabled";

    protected final BeanProvider<ManagementService> providerManagementService;

    protected final Map<String, MetricsQuery> metricsQueryMap = new HashMap<>();

    protected CamundaMetrics(BeanProvider<ManagementService> providerManagementService) {
        this.providerManagementService = providerManagementService;
    }

    protected long sumMetric(String metricName) {
        MetricsQuery metricsQuery = getMetricsQuery(metricName);
        if (metricsQuery != null) {
            return metricsQuery.sum();
        }
        throw new BeanInstantiationException("Camunda MetricsQuery is null");
    }

    protected MetricsQuery getMetricsQuery(String metricName) {
        if (metricsQueryMap.containsKey(metricName)) {
            return metricsQueryMap.get(metricName);
        }
        MetricsQuery metricsQuery = providerManagementService.get().createMetricsQuery().name(metricName);
        metricsQueryMap.put(metricName, metricsQuery); // does not return the initialized metrics query
        return metricsQuery;
    }

}
