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
package info.novatec.micronaut.camunda.bpm.feature.metrics.test

import info.novatec.micronaut.camunda.bpm.feature.metrics.*
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.inject.Inject

@MicronautTest
@Requires(beans = [Server::class])
internal class CamundaMetricsTest {

    @Inject
    lateinit var applicationContext: ApplicationContext

    @Inject
    @field:Client("/")
    lateinit var client: RxHttpClient

    @Test
    fun `all Camunda Metrics should be available, when enabled in application-yml`() {
        assertThat(applicationContext.findBean(BpmnExecutionMetrics::class.java)).isPresent
        assertThat(applicationContext.findBean(BpmnExecutionMetricsBinder::class.java)).isPresent
        assertThat(applicationContext.findBean(DmnExecutionMetrics::class.java)).isPresent
        assertThat(applicationContext.findBean(DmnExecutionMetricsBinder::class.java)).isPresent
        assertThat(applicationContext.findBean(HistoryCleanUpMetrics::class.java)).isPresent
        assertThat(applicationContext.findBean(HistoryCleanUpMetricsBinder::class.java)).isPresent
        assertThat(applicationContext.findBean(JobExecutorMetrics::class.java)).isPresent
        assertThat(applicationContext.findBean(JobExecutorMetricsBinder::class.java)).isPresent
    }

    @Test
    fun `should return all camunda metric names, when prometheus endpoint is called`() {
        val request: HttpRequest<String> = GET("/prometheus")
        val body = client.toBlocking().retrieve(request)

        assertThat(body).contains(
                "camunda_bpmn_activity_instance_end",
                "camunda_bpmn_activity_instance_start",
                "camunda_bpmn_root_process_instance_start",
                "camunda_dmn_executed_decision_elements",
                "camunda_dmn_executed_decision_instances",
                "camunda_history_cleanup_removed_batch_operations",
                "camunda_history_cleanup_removed_decision_instances",
                "camunda_history_cleanup_removed_process_instances",
                "camunda_job_acquired_failure",
                "camunda_job_acquired_success",
                "camunda_job_acquisition_attempt",
                "camunda_job_execution_rejected",
                "camunda_job_failed",
                "camunda_job_locked_exclusive",
                "camunda_job_successful"
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "camunda.bpmn.activity.instance.end",
        "camunda.bpmn.activity.instance.start",
        "camunda.bpmn.root.process.instance.start",
        "camunda.dmn.executed.decision.elements",
        "camunda.dmn.executed.decision.instances",
        "camunda.history.cleanup.removed.batch.operations",
        "camunda.history.cleanup.removed.decision.instances",
        "camunda.history.cleanup.removed.process.instances",
        "camunda.job.acquired.failure",
        "camunda.job.acquired.success",
        "camunda.job.acquisition.attempt",
        "camunda.job.execution.rejected",
        "camunda.job.failed",
        "camunda.job.locked.exclusive",
        "camunda.job.successful"
    ])
    fun `should return zero, when camunda metrics endpoint is called`(metricName: String) {
        val request: HttpRequest<String> = GET("/metrics/$metricName")
        val body = client.toBlocking().retrieve(request)

        assertThatJson(body).inPath("measurements[0].statistic").isString.isEqualTo("VALUE")
        assertThatJson(body).inPath("measurements[0].value").isNumber.asString().isEqualTo("0.0")
    }
}