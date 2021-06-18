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
import jakarta.inject.Singleton;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.interceptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.micronaut.transaction.TransactionDefinition.Propagation.REQUIRED;
import static io.micronaut.transaction.TransactionDefinition.Propagation.REQUIRES_NEW;

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

    protected final List<ProcessEnginePlugin> plugins;

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
                                        List<ProcessEnginePlugin> plugins,
                                        ProcessEngineConfigurationCustomizer processEngineConfigurationCustomizer) {
        this.transactionManager = transactionManager;
        this.jobExecutor = jobExecutor;
        this.telemetryRegistry = telemetryRegistry;
        this.beansResolverFactory = beansResolverFactory;
        this.environment = environment;
        this.camundaVersion = camundaVersion;
        this.basicJdbcConfiguration = basicJdbcConfiguration;
        this.plugins = plugins;
        checkForDeprecatedConfiguration();
        setDataSource(dataSource);
        setTransactionsExternallyManaged(true);
        setExpressionManager(new MnExpressionManager(new ApplicationContextElResolver(applicationContext)));
        setArtifactFactory(artifactFactory);

        configureDefaultValues();

        applyGenericProperties(configuration);

        configureTelemetry();

        registerProcessEnginePlugins();

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
                    .orElseThrow(() -> new RuntimeException("Invalid process engine property: " + entry.getKey()));

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
        } else if (type == boolean.class || type == Boolean.class) {
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

    protected void registerProcessEnginePlugins() {
        log.info("Registering process engine plugins: {}", plugins);
        setProcessEnginePlugins(plugins);
    }

}
