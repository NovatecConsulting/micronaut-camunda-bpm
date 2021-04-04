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
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetrics.MICRONAUT_METRICS_BINDERS_CAMUNDA_JOB_EXECUTOR_ENABLED;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.JOB_EXECUTOR_DEFAULT_TAGS;
import static info.novatec.micronaut.camunda.bpm.feature.metrics.CamundaMetricsBinderTags.TAG_KEY_NAME;
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Provides Metrics about Job Execution.
 */
@Singleton
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS_CAMUNDA_JOB_EXECUTOR_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Requires(property = CAMUNDA_GENERIC_PROPERTIES_PROPERTIES_METRICS_ENABLED, notEquals = StringUtils.FALSE)
public class JobExecutorMetricsBinder implements MeterBinder {

    protected final JobExecutorMetrics jobExecutorMetrics;

    public JobExecutorMetricsBinder(JobExecutorMetrics jobExecutorMetrics) {
        this.jobExecutorMetrics = jobExecutorMetrics;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {

        Gauge.builder(
            createMetricName(Metrics.JOB_SUCCESSFUL),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobSuccessful
        )
            .description("The number of jobs successfully executed.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_SUCCESSFUL))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.JOB_FAILED),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobFailed
        )
            .description("The number of jobs that failed to execute and that were submitted for retry. Every failed attempt to execute a job is counted.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_FAILED))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.JOB_ACQUISITION_ATTEMPT),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobAcquisitionAttempt
        )
            .description("The number of job acquisition cycles performed.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_ACQUISITION_ATTEMPT))
            .register(registry);
        Gauge.builder(
            createMetricName(Metrics.JOB_ACQUIRED_SUCCESS),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobAcquiredSuccess
        )
            .description("The number of jobs that were acquired and successfully locked for execution.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_ACQUIRED_SUCCESS))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.JOB_ACQUIRED_FAILURE),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobAcquiredFailure
        )
            .description("The number of jobs that were acquired but could not be locked for execution due to another job executor locking/executing the jobs in parallel.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_ACQUIRED_FAILURE))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.JOB_EXECUTION_REJECTED),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobExecutionRejected
        )
            .description("The number of successfully acquired jobs submitted for execution that were rejected due to saturated execution resources. This is an indicator that the execution thread pool's job queue is full.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_EXECUTION_REJECTED))
            .register(registry);

        Gauge.builder(
            createMetricName(Metrics.JOB_LOCKED_EXCLUSIVE),
            jobExecutorMetrics,
            JobExecutorMetrics::countJobLockedExclusive
        )
            .description("The number of exclusive jobs that are immediately locked and executed.")
            .tags(Tags.concat(JOB_EXECUTOR_DEFAULT_TAGS, TAG_KEY_NAME, Metrics.JOB_LOCKED_EXCLUSIVE))
            .register(registry);
    }

    private String createMetricName(String camundaName) {
        return "camunda." + camundaName.replace('-', '.');
    }

}
