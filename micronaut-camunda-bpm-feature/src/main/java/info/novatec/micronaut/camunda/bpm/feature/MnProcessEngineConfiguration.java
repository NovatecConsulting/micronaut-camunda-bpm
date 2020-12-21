package info.novatec.micronaut.camunda.bpm.feature;

import info.novatec.micronaut.camunda.bpm.feature.tx.MnTransactionContextFactory;
import info.novatec.micronaut.camunda.bpm.feature.tx.MnTransactionInterceptor;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.transaction.SynchronousTransactionManager;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.impl.interceptor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.micronaut.transaction.TransactionDefinition.Propagation.REQUIRED;
import static io.micronaut.transaction.TransactionDefinition.Propagation.REQUIRES_NEW;

/**
 * Micronaut implementation of {@link org.camunda.bpm.engine.ProcessEngineConfiguration} which is aware of transaction
 * management, i.e. the surrounding transaction will be used and {@link org.camunda.bpm.engine.delegate.JavaDelegate}s
 * are executed in a transaction allowing the persistence of data with micronaut-data.
 *
 * @author Tobias Schäfer
 * @author Lukasz Frankowski
 */
@Singleton
public class MnProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

    private static final Logger log = LoggerFactory.getLogger(MnProcessEngineConfiguration.class);

    protected final SynchronousTransactionManager<Connection> transactionManager;

    protected final MnJobExecutor jobExecutor;

    protected final Configuration configuration;

    protected final MnTelemetryRegistry telemetryRegistry;

    protected final Environment environment;

    public MnProcessEngineConfiguration(SynchronousTransactionManager<Connection> transactionManager,
                                        MnJobExecutor jobExecutor,
                                        Configuration configuration,
                                        MnTelemetryRegistry telemetryRegistry,
                                        Environment environment,
                                        ApplicationContext applicationContext,
                                        DataSource dataSource,
                                        MnArtifactFactory artifactFactory,
                                        ProcessEngineConfigurationCustomizer processEngineConfigurationCustomizer) {
        this.transactionManager = transactionManager;
        this.jobExecutor = jobExecutor;
        this.configuration = configuration;
        this.telemetryRegistry = telemetryRegistry;
        this.environment = environment;
        setDataSource(dataSource);
        setTransactionsExternallyManaged(true);
        setDatabaseSchemaUpdate(configuration.getDatabase().getSchemaUpdate());
        setHistory(configuration.getHistoryLevel());
        setJobExecutorActivate(true);
        setExpressionManager(new MnExpressionManager(new ApplicationContextElResolver(applicationContext)));
        setArtifactFactory(artifactFactory);

        configureTelemetry();

        processEngineConfigurationCustomizer.customize(this);
    }

    @Override
    public ProcessEngine buildProcessEngine() {
        return transactionManager.executeWrite(
            transactionStatus -> {
                log.info("Building process engine connected to {}", dataSource.getConnection().getMetaData().getURL());
                return super.buildProcessEngine();
            }
        );
    }

    @Override
    public HistoryLevel getDefaultHistoryLevel() {
        // Define default history level for history level "auto".
        return HistoryLevel.HISTORY_LEVEL_FULL;
    }

    @Override
    protected void initTransactionContextFactory() {
        if(transactionContextFactory == null) {
            transactionContextFactory = new MnTransactionContextFactory(transactionManager);
        }
    }

    @Override
    protected void initJobExecutor() {
        setJobExecutor(jobExecutor);
        super.initJobExecutor();
    }

    @Override
    protected Collection< ? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequired() {
        return getCommandInterceptors(false);
    }

    @Override
    protected Collection< ? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequiresNew() {
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

    /**
     * Configure telemetry based on configuration but always disable if the "test" profile is active,
     * i.e. tests are being executed.
     */
    protected void configureTelemetry() {
        boolean testProfileActive = environment.getActiveNames().contains("test");
        setTelemetryReporterActivate(!testProfileActive && configuration.getTelemetry().isTelemetryReporterActivate());
        if (!testProfileActive && configuration.getTelemetry().isInitializeTelemetry()) {
            setInitializeTelemetry(true);
        }
        setTelemetryRegistry(telemetryRegistry);
    }
}
