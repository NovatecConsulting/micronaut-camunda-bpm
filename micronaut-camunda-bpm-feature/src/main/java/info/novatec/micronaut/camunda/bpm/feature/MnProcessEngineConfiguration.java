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

import info.novatec.micronaut.camunda.bpm.feature.tx.MnTransactionContextFactory;
import info.novatec.micronaut.camunda.bpm.feature.tx.MnTransactionInterceptor;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.transaction.SynchronousTransactionManager;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.history.*;
import org.camunda.bpm.engine.impl.*;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cmmn.CaseServiceImpl;
import org.camunda.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionQueryImpl;
import org.camunda.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionQueryImpl;
import org.camunda.bpm.engine.impl.cmmn.entity.runtime.CaseInstanceQueryImpl;
import org.camunda.bpm.engine.impl.interceptor.*;
import org.camunda.bpm.engine.repository.CaseDefinition;
import org.camunda.bpm.engine.repository.CaseDefinitionQuery;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseExecutionQuery;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.runtime.CaseInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static io.micronaut.transaction.TransactionDefinition.Propagation.REQUIRED;
import static io.micronaut.transaction.TransactionDefinition.Propagation.REQUIRES_NEW;
import static java.util.Collections.*;

/**
 * Micronaut implementation of {@link org.camunda.bpm.engine.ProcessEngineConfiguration} which is aware of transaction
 * management, i.e. the surrounding transaction will be used and {@link org.camunda.bpm.engine.delegate.JavaDelegate}s
 * are executed in a transaction allowing the persistence of data with micronaut-data.
 *
 * @author Tobias Schäfer
 * @author Lukasz Frankowski
 * @author Titus Meyer
 */
@Singleton
@Introspected
public class MnProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

    private static final Logger log = LoggerFactory.getLogger(MnProcessEngineConfiguration.class);

    protected final SynchronousTransactionManager<Connection> transactionManager;

    protected final MnJobExecutor jobExecutor;

    protected final MnTelemetryRegistry telemetryRegistry;

    protected final MnBeansResolverFactory beansResolverFactory;

    protected final Environment environment;

    protected final CamundaVersion camundaVersion;

    protected final BasicJdbcConfiguration basicJdbcConfiguration;

    public MnProcessEngineConfiguration(SynchronousTransactionManager<Connection> transactionManager,
                                        MnJobExecutor jobExecutor,
                                        Configuration configuration,
                                        MnTelemetryRegistry telemetryRegistry,
                                        Environment environment,
                                        CamundaVersion camundaVersion,
                                        ApplicationContext applicationContext,
                                        BasicJdbcConfiguration basicJdbcConfiguration,
                                        DataSource dataSource,
                                        MnArtifactFactory artifactFactory,
                                        MnBeansResolverFactory beansResolverFactory,
                                        ProcessEngineConfigurationCustomizer processEngineConfigurationCustomizer) {
        this.transactionManager = transactionManager;
        this.jobExecutor = jobExecutor;
        this.telemetryRegistry = telemetryRegistry;
        this.beansResolverFactory = beansResolverFactory;
        this.environment = environment;
        this.camundaVersion = camundaVersion;
        this.basicJdbcConfiguration = basicJdbcConfiguration;
        checkForDeprecatedConfiguration();
        mockUnsupportedCmmnMethods();
        setDataSource(dataSource);
        setTransactionsExternallyManaged(true);
        setExpressionManager(new MnExpressionManager(new ApplicationContextElResolver(applicationContext)));
        setArtifactFactory(artifactFactory);

        configureDefaultValues();

        applyGenericProperties(configuration);

        configureTelemetry();

        processEngineConfigurationCustomizer.customize(this);
    }

    @Override
    public ProcessEngine buildProcessEngine() {
        return transactionManager.executeWrite(
                transactionStatus -> {
                    log.info("Building process engine connected to {}", basicJdbcConfiguration.getUrl());
                    Instant start = Instant.now();
                    ProcessEngine processEngine = super.buildProcessEngine();
                    log.info("Started process engine in {}ms", ChronoUnit.MILLIS.between(start, Instant.now()));
                    return processEngine;
                }
        );
    }

    @Override
    protected void initTransactionContextFactory() {
        if (transactionContextFactory == null) {
            transactionContextFactory = new MnTransactionContextFactory(transactionManager);
        }
    }

    @Override
    protected void initJobExecutor() {
        setJobExecutor(jobExecutor);
        super.initJobExecutor();
    }

    @Override
    protected void initScripting() {
        super.initScripting();
        // make Micronaut context managed beans available for scripting
        getResolverFactories().add(beansResolverFactory);
    }

    @Override
    protected InputStream getMyBatisXmlConfigurationSteam() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(super.getMyBatisXmlConfigurationSteam()));
        try {
            StringBuilder sb = new StringBuilder();
            while (reader.ready()) {
                String line = reader.readLine();
                if (!line.contains("<mapper resource=\"org/camunda/bpm/engine/impl/mapping/entity/Case")) {
                    sb.append(line);
                    sb.append("\n");
                } else {
                    log.debug("Filtered out CMMN mapping {}", line);
                }
            }
            return new ByteArrayInputStream(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Collection<? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequired() {
        return getCommandInterceptors(false);
    }

    @Override
    protected Collection<? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequiresNew() {
        return getCommandInterceptors(true);
    }

    protected List<CommandInterceptor> getCommandInterceptors(boolean requiresNew) {
        return Arrays.asList(
                new LogInterceptor(),
                new CommandCounterInterceptor(this),
                new ProcessApplicationContextInterceptor(this),
                new MnTransactionInterceptor(transactionManager, requiresNew ? REQUIRES_NEW : REQUIRED),
                new CommandContextInterceptor(commandContextFactory, this, requiresNew)
        );
    }

    protected void checkForDeprecatedConfiguration() {
        if (!environment.getPropertyEntries("camunda.bpm").isEmpty()) {
            String msg = "All properties with the prefix 'camunda.bpm.*' have been renamed to 'camunda.*'. Please update your configuration!";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Mocks methods which must work although we removed all references to CMMN.
     */
    protected void mockUnsupportedCmmnMethods() {
        repositoryService = new RepositoryServiceImpl() {
            @Override
            public CaseDefinitionQuery createCaseDefinitionQuery() {
                return new CaseDefinitionQueryImpl(commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        // This method is called by the Cockpit's start page
                        return 0;
                    }

                    @Override
                    public List<CaseDefinition> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }
        };
        caseService = new CaseServiceImpl() {
            @Override
            public CaseInstanceQuery createCaseInstanceQuery() {
                return new CaseInstanceQueryImpl(commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        return 0;
                    }

                    @Override
                    public List<CaseInstance> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }

            @Override
            public CaseExecutionQuery createCaseExecutionQuery() {
                return new CaseExecutionQueryImpl(commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        return 0;
                    }

                    @Override
                    public List<CaseExecution> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }
        };
        historyService = new HistoryServiceImpl() {
            @Override
            public HistoricCaseInstanceQuery createHistoricCaseInstanceQuery() {
                return new HistoricCaseInstanceQueryImpl(commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        return 0;
                    }

                    @Override
                    public List<HistoricCaseInstance> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }

            @Override
            public HistoricCaseActivityInstanceQuery createHistoricCaseActivityInstanceQuery() {
                return new HistoricCaseActivityInstanceQueryImpl(commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        return 0;
                    }

                    @Override
                    public List<HistoricCaseActivityInstance> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }

            @Override
            public HistoricCaseActivityStatisticsQuery createHistoricCaseActivityStatisticsQuery(String caseDefinitionId) {
                return new HistoricCaseActivityStatisticsQueryImpl(caseDefinitionId, commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        return 0;
                    }

                    @Override
                    public List<HistoricCaseActivityStatistics> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }

            @Override
            public CleanableHistoricCaseInstanceReport createCleanableHistoricCaseInstanceReport() {
                return new CleanableHistoricCaseInstanceReportImpl(commandExecutor) {
                    @Override
                    public long executeCount(CommandContext commandContext) {
                        return 0;
                    }

                    @Override
                    public List<CleanableHistoricCaseInstanceReportResult> executeList(CommandContext commandContext, Page page) {
                        return emptyList();
                    }
                };
            }
        };
    }

    /**
     * Configure telemetry registry and always disable if the "test" profile is active, i.e. tests are being executed.
     */
    protected void configureTelemetry() {
        setTelemetryRegistry(telemetryRegistry);
        if (environment.getActiveNames().contains(Environment.TEST)) {
            setInitializeTelemetry(false);
            setTelemetryReporterActivate(false);
        } else if ( Boolean.TRUE.equals(isInitializeTelemetry()) && isTelemetryReporterActivate() && !camundaVersion.getVersion().isPresent() ) {
            log.warn("Disabling TelemetryReporter because required information 'Camunda Version' is not available.");
            setTelemetryReporterActivate(false);
        }
    }

    /**
     * Configure sensible defaults so that the user must not take care of it.
     */
    protected void configureDefaultValues() {
        setJobExecutorActivate(!environment.getActiveNames().contains(Environment.TEST));
        setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    }

    protected void applyGenericProperties(Configuration configuration) {
        BeanIntrospection<MnProcessEngineConfiguration> introspection = BeanIntrospection.getIntrospection(MnProcessEngineConfiguration.class);

        for (Map.Entry<String, Object> entry : configuration.getGenericProperties().getProperties().entrySet()) {
            BeanProperty<MnProcessEngineConfiguration, Object> property = introspection.getProperty(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Invalid engine property: " + entry.getKey()));

            property.set(this, resolveGenericPropertyValue(entry.getValue(), property.getType()));
        }
    }

    protected Object resolveGenericPropertyValue(Object value, Class<?> type) {
        // Even if the value is not of type String we cannot assume that it is of the correct type,
        // e.g. a value of "30" will have the type "int" and can then not be set as a value if the
        // configuration is of type "long".
        // Therefore we always use the string value for primitive types and convert it to the target type.
        if (type == int.class) {
            return Integer.valueOf(String.valueOf(value));
        } else if (type == long.class) {
            return Long.valueOf(String.valueOf(value));
        } else if (type == boolean.class) {
            return Boolean.valueOf(String.valueOf(value));
        } else if (type == String.class) {
            return String.valueOf(value);
        } else {
            return value;
        }
    }

    public ProcessEngineConfigurationImpl setInitializeTelemetry(Boolean telemetryInitialized) {
        // This method makes the Boolean telemetryInitialized a writable property.
        // Otherwise applyGenericProperties cannot set the property.
        return super.setInitializeTelemetry(telemetryInitialized);
    }
}
