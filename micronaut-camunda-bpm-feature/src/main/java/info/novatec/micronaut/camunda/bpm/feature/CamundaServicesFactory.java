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

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.camunda.bpm.engine.*;

/**
 * This factory provides all Camunda Services based on the current @{@link ProcessEngine}.
 *
 * @author Tobias Schäfer
 * @author Lukasz Frankowski
 */
@Factory
public class CamundaServicesFactory {

    /**
     * Creates a bean for the {@link RuntimeService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link RuntimeService}
     */
    @Singleton
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    /**
     * Creates a bean for the {@link RepositoryService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link RepositoryService}
     */
    @Singleton
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * Creates a bean for the {@link ManagementService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link ManagementService}
     */
    @Singleton
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    /**
     * Creates a bean for the {@link AuthorizationService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link AuthorizationService}
     */
    @Singleton
    public AuthorizationService authorizationService(ProcessEngine processEngine) {
        return processEngine.getAuthorizationService();
    }

    /**
     * Creates a bean for the {@link CaseService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link CaseService}
     */
    @Singleton
    public CaseService caseService(ProcessEngine processEngine) {
        return processEngine.getCaseService();
    }

    /**
     * Creates a bean for the {@link DecisionService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link DecisionService}
     */
    @Singleton
    public DecisionService decisionService(ProcessEngine processEngine) {
        return processEngine.getDecisionService();
    }

    /**
     * Creates a bean for the {@link ExternalTaskService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link ExternalTaskService}
     */
    @Singleton
    public ExternalTaskService externalTaskService(ProcessEngine processEngine) {
        return processEngine.getExternalTaskService();
    }

    /**
     * Creates a bean for the {@link FilterService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link FilterService}
     */
    @Singleton
    public FilterService filterService(ProcessEngine processEngine) {
        return processEngine.getFilterService();
    }

    /**
     * Creates a bean for the {@link FormService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link FormService}
     */
    @Singleton
    public FormService formService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    /**
     * Creates a bean for the {@link TaskService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link TaskService}
     */
    @Singleton
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    /**
     * Creates a bean for the {@link HistoryService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link HistoryService}
     */
    @Singleton
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    /**
     * Creates a bean for the {@link IdentityService} in the application context which can be injected if needed.
     *
     * @param processEngine the {@link ProcessEngine}
     * @return the {@link IdentityService}
     */
    @Singleton
    public IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }
}
