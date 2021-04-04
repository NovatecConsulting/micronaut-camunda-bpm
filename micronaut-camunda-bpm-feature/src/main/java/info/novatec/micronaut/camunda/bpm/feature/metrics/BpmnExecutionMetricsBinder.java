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
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.MICRONAUT_METRICS_BINDERS_CAMUNDA_BPMN_EXECUTION_ENABLED;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.BPMN_EXECUTION_DEFAULT_TAGS;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.TAG_KEY_NAME;
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Provides Metrics about BPMN Execution.
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS_CAMUNDA_BPMN_EXECUTION_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Requires(property = CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED, notEquals = StringUtils.FALSE)
public class BpmnExecutionMetricsBinder implements MeterBinder {

    protected final BpmnExecutionMetrics bpmnExecutionMetrics;

    public BpmnExecutionMetricsBinder(BpmnExecutionMetrics bpmnExecutionMetrics) {
        this.bpmnExecutionMetrics = bpmnExecutionMetrics;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {

        /*
          This Code

          Gauge.builder("camunda.bpmn." + Metrics.ROOT_PROCESS_INSTANCE_START.replace('-', '.'),
                providerManagementService.get().createMetricsQuery().name(Metrics.ROOT_PROCESS_INSTANCE_START),
                MetricsQuery::sum
          ).description("The number of root process instance executions started. This is also known as Root Process Instances (RPI). A root process instance has no parent process instance, i.e. it is a top-level execution.")
           .tags(Tags.concat(BPMN_EXECUTION_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.ROOT_PROCESS_INSTANCE_START))
           .register(registry);

          is executed to early and the bean for providerManagementService is not instanciated
          so that we would run into a boot loop and then the application crashes.

          To avoid this, we implemented some "holder" objects. In this case info.novatec.micronaut.camunda.bpm.feature.metrics.BpmnExecutionMetrics
        */
        Gauge.builder(
            createMetricName(Metrics.ROOT_PROCESS_INSTANCE_START),
            bpmnExecutionMetrics,
            BpmnExecutionMetrics::countRootProcessInstanceStart
        )
            .description("The number of root process instance executions started. This is also known as Root Process Instances (RPI). A root process instance has no parent process instance, i.e. it is a top-level execution.")
            .tags(Tags.concat(BPMN_EXECUTION_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.ROOT_PROCESS_INSTANCE_START))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.ACTIVTY_INSTANCE_START),
            bpmnExecutionMetrics,
            BpmnExecutionMetrics::countActivityInstanceStart
        )
            .description("The number of activity instances started. This is also known as flow node instances (FNI).")
            .tags(Tags.concat(BPMN_EXECUTION_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.ACTIVTY_INSTANCE_START))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.ACTIVTY_INSTANCE_END),
            bpmnExecutionMetrics,
            BpmnExecutionMetrics::countActivityInstanceEnd
        )
            .description("The number of activity instances ended.")
            .tags(Tags.concat(BPMN_EXECUTION_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.ACTIVTY_INSTANCE_END))
            .register(registry);
    }

    private String createMetricName(String camundaName) {
        return "camunda.bpmn." + camundaName.replace('-', '.');
    }

}
