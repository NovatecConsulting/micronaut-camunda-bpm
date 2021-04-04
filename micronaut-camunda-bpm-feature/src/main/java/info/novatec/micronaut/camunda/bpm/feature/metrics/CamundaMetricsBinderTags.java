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

import io.micrometer.core.instrument.Tag;

import java.util.Arrays;

/**
 * Constants for our MetricsBinders.
 */
public final class CamundaMetricsBinderTags {

    static final String TAG_KEY_NAME = "name";

    static final String TAG_KEY_CATEGORY = "category";

    static final Tag TAG_CLASS_CAMUNDA = Tag.of("class", "camunda");

    static final Iterable<Tag> JOB_EXECUTOR_DEFAULT_TAGS = Arrays.asList(TAG_CLASS_CAMUNDA, Tag.of(TAG_KEY_CATEGORY, "job-executor"));

    static final Iterable<Tag> DMN_EXECUTION_DEFAULT_TAGS = Arrays.asList(TAG_CLASS_CAMUNDA, Tag.of(TAG_KEY_CATEGORY, "dmn-execution"));

    static final Iterable<Tag> BPMN_EXECUTION_DEFAULT_TAGS = Arrays.asList(TAG_CLASS_CAMUNDA, Tag.of(TAG_KEY_CATEGORY, "bpmn-execution"));

    static final Iterable<Tag> HISTORY_CLEAN_UP_DEFAULT_TAGS = Arrays.asList(TAG_CLASS_CAMUNDA, Tag.of(TAG_KEY_CATEGORY, "history-clean-up"));

    private CamundaMetricsBinderTags() {}
}
