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
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import javax.inject.Inject


@MicronautTest
internal class CamundaMetricsNotEnabledByDefaultTest {

    @Inject
    lateinit var applicationContext: ApplicationContext

    @Test
    fun `Camunda Metrics should not be available, when only micrometer jar is included`() {
        // meter registry is available
        assertTrue(applicationContext.findBean(MeterRegistry::class.java).isPresent)

        // all provided camunda metrics are not available by default
        assertFalse(applicationContext.findBean(BpmnExecutionMetrics::class.java).isPresent)
        assertFalse(applicationContext.findBean(BpmnExecutionMetricsBinder::class.java).isPresent)
        assertFalse(applicationContext.findBean(DmnExecutionMetrics::class.java).isPresent)
        assertFalse(applicationContext.findBean(DmnExecutionMetricsBinder::class.java).isPresent)
        assertFalse(applicationContext.findBean(HistoryCleanUpMetrics::class.java).isPresent)
        assertFalse(applicationContext.findBean(HistoryCleanUpMetricsBinder::class.java).isPresent)
        assertFalse(applicationContext.findBean(JobExecutorMetrics::class.java).isPresent)
        assertFalse(applicationContext.findBean(JobExecutorMetricsBinder::class.java).isPresent)
    }
}