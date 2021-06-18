/*
 * Copyright 2020-2021 original authors
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
package info.novatec.micronaut.camunda.bpm.feature;

import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Micronaut specific implementation of the {@link org.camunda.bpm.engine.impl.jobexecutor.JobExecutor}
 *
 * @author Tobias Schäfer
 * @author Alexander Rolfes
 */
@Singleton
// Implementation based on https://github.com/camunda/camunda-bpm-platform/blob/master/engine-spring/core/src/main/java/org/camunda/bpm/engine/spring/components/jobexecutor/SpringJobExecutor.java
public class MnJobExecutor extends JobExecutor {

    protected final ExecutorService ioExecutor;

    public MnJobExecutor(@Named(TaskExecutors.IO) ExecutorService ioExecutor, JobExecutorCustomizer jobExecutorCustomizer) {
        this.ioExecutor = ioExecutor;
        jobExecutorCustomizer.customize(this);
    }

    @Override
    protected void startExecutingJobs() {
        startJobAcquisitionThread();
    }

    @Override
    protected void stopExecutingJobs() {
        stopJobAcquisitionThread();
    }

    @Override
    public void executeJobs(List<String> jobIds, ProcessEngineImpl processEngine) {
        try {
            ioExecutor.execute(getExecuteJobsRunnable(jobIds, processEngine));
        } catch (RejectedExecutionException e) {
            logRejectedExecution(processEngine, jobIds.size());
            rejectedJobsHandler.jobsRejected(jobIds, processEngine, this);
        }
    }

}
