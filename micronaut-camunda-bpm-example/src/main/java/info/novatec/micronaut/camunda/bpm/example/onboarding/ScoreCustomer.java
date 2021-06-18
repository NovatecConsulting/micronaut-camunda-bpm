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
package info.novatec.micronaut.camunda.bpm.example.onboarding;

import jakarta.inject.Singleton;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

@Singleton
public class ScoreCustomer implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ScoreCustomer.class);

    @Override
    public void execute(DelegateExecution execution) {
        int scoring = ThreadLocalRandom.current().nextInt(1, 100);
        log.info("Scored {} with result {}.", execution.getBusinessKey(), scoring);
        execution.setVariable("result", scoring);
    }
}
