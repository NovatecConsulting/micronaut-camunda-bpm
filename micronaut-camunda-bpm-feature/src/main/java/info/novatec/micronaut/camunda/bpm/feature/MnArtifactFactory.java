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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.NoSuchBeanException;
import jakarta.inject.Singleton;
import org.camunda.bpm.engine.ArtifactFactory;
import org.camunda.bpm.engine.impl.DefaultArtifactFactory;

/**
 * Micronaut specific implementation of {@link ArtifactFactory} to resolve classes from the {@link ApplicationContext}.
 *
 * @author Tobias Schäfer
 */
@Singleton
public class MnArtifactFactory implements ArtifactFactory {

    protected final ApplicationContext applicationContext;

    protected final ArtifactFactory defaultArtifactFactory = new DefaultArtifactFactory();

    public MnArtifactFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getArtifact(Class<T> clazz) {
        try {
            return applicationContext.getBean(clazz);
        } catch (NoSuchBeanException ex) {
            // fall back to default implementation
            return defaultArtifactFactory.getArtifact(clazz);
        }
    }
}
