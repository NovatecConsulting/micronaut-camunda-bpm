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
package info.novatec.micronaut.camunda.bpm.feature.tx;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.transaction.SynchronousTransactionManager;
import io.micronaut.transaction.support.TransactionSynchronization;
import io.micronaut.transaction.support.TransactionSynchronizationAdapter;
import io.micronaut.transaction.support.TransactionSynchronizationManager;
import org.camunda.bpm.engine.impl.cfg.TransactionContext;
import org.camunda.bpm.engine.impl.cfg.TransactionListener;
import org.camunda.bpm.engine.impl.cfg.TransactionState;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;

import java.sql.Connection;

import static org.camunda.bpm.engine.impl.cfg.TransactionState.*;

/**
 * Micronaut specific implementation of {@link TransactionContext}
 *
 * @author Tobias Schäfer
 */
// Implementation based on https://github.com/camunda/camunda-bpm-platform/blob/master/engine-spring/core/src/main/java/org/camunda/bpm/engine/spring/SpringTransactionContext.java
public class MnTransactionContext implements TransactionContext {

    protected final SynchronousTransactionManager<Connection> transactionManager;
    protected final CommandContext commandContext;
    protected TransactionState lastTransactionState = null;

    public MnTransactionContext(CommandContext commandContext, SynchronousTransactionManager<Connection> transactionManager) {
        this.commandContext = commandContext;
        this.transactionManager = transactionManager;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void beforeCommit(boolean readOnly) {
                lastTransactionState = COMMITTING;
            }

            @Override
            public void afterCommit() {
                lastTransactionState = COMMITTED;
            }

            @Override
            public void beforeCompletion() {
                lastTransactionState = ROLLINGBACK;
            }

            @Override
            public void afterCompletion(@NonNull Status status) {
                if (Status.ROLLED_BACK == status) {
                    lastTransactionState = ROLLED_BACK;
                }
            }
        });
    }

    @Override
    public void commit() {
        // managed transaction, ignore
    }

    @Override
    public void rollback() {
        // Just in case the rollback isn't triggered by an
        // exception, we mark the current transaction rollBackOnly.
        transactionManager.getTransaction(null).setRollbackOnly();
    }

    @Override
    public void addTransactionListener(TransactionState transactionState, TransactionListener transactionListener) {
        TransactionSynchronization transactionSynchronization;
        switch (transactionState) {
            case COMMITTING:
                transactionSynchronization = new TransactionSynchronizationAdapter() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        transactionListener.execute(commandContext);
                    }
                };
                break;
            case COMMITTED:
                transactionSynchronization = new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        transactionListener.execute(commandContext);
                    }
                };
                break;
            case ROLLINGBACK:
                transactionSynchronization = new TransactionSynchronizationAdapter() {
                    @Override
                    public void beforeCompletion() {
                        transactionListener.execute(commandContext);
                    }
                };
                break;
            case ROLLED_BACK:
                transactionSynchronization = new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(@NonNull Status status) {
                        if (Status.ROLLED_BACK == status) {
                            transactionListener.execute(commandContext);
                        }
                    }
                };
                break;
            default:
                throw new IllegalStateException("Unknown transaction state: " + transactionState);
        }
        TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
    }

    @Override
    public boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && ROLLED_BACK != lastTransactionState
                && ROLLINGBACK != lastTransactionState;
    }
}
