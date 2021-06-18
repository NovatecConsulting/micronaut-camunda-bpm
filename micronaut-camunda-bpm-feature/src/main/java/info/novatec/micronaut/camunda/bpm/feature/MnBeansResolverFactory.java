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
package info.novatec.micronaut.camunda.bpm.feature;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.naming.NameResolver;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.scripting.engine.Resolver;
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link ResolverFactory} and {@link Resolver} implementations to make the beans
 * managed by Micronaut available in Camunda scripting.
 *
 * @author Titus Meyer
 * @see info.novatec.micronaut.camunda.bpm.feature.MnProcessEngineConfiguration#initScripting()
 */
// Implementation based on: https://github.com/camunda/camunda-bpm-platform/blob/master/engine-spring/core/src/main/java/org/camunda/bpm/engine/spring/SpringBeansResolverFactory.java
@Singleton
public class MnBeansResolverFactory implements ResolverFactory, Resolver {

    protected final ApplicationContext applicationContext;
    protected Set<String> keySet;

    public MnBeansResolverFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof String && getKeySet().contains(key);
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            Qualifier<Object> qualifier = Qualifiers.byName((String) key);
            if (applicationContext.containsBean(Object.class, qualifier)) {
                return applicationContext.getBean(Object.class, qualifier);
            }
        }
        return null;
    }

    @Override
    public Set<String> keySet() {
        return getKeySet();
    }

    @Override
    public Resolver createResolver(VariableScope variableScope) {
        return this;
    }

    protected synchronized Set<String> getKeySet() {
        if (keySet == null) {
            keySet = applicationContext.getAllBeanDefinitions().stream()
                    .map(this::getBeanName)
                    .collect(Collectors.toSet());
        }
        return keySet;
    }

    protected String getBeanName(BeanDefinition<?> beanDefinition) {
        // Inspired by NameQualifier.reduce()..
        Optional<String> beanQualifier = beanDefinition.getAnnotationMetadata()
                .findDeclaredAnnotation(Named.class)
                .flatMap(AnnotationValue::stringValue);
        return beanQualifier.orElseGet(() -> {
            if (beanDefinition instanceof NameResolver) {
                Optional<String> resolvedName = ((NameResolver) beanDefinition).resolveName();
                return resolvedName.orElse(getBeanNameFromType(beanDefinition));
            }
            return getBeanNameFromType(beanDefinition);
        });
    }

    protected String getBeanNameFromType(BeanDefinition<?> beanDefinition) {
        String beanName = beanDefinition.getBeanType().getSimpleName();
        // lower the first character
        return Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
    }
}
