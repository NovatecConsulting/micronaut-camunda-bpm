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

import info.novatec.micronaut.camunda.bpm.feature.rest.RestApp;
import info.novatec.micronaut.camunda.bpm.feature.webapp.*;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import org.camunda.bpm.admin.impl.web.bootstrap.AdminContainerBootstrap;
import org.camunda.bpm.cockpit.impl.web.bootstrap.CockpitContainerBootstrap;
import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.engine.rest.filter.CacheControlFilter;
import org.camunda.bpm.engine.rest.filter.EmptyBodyFilter;
import org.camunda.bpm.tasklist.impl.web.bootstrap.TasklistContainerBootstrap;
import org.camunda.bpm.webapp.impl.engine.ProcessEnginesFilter;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.camunda.bpm.webapp.impl.security.filter.CsrfPreventionFilter;
import org.camunda.bpm.webapp.impl.security.filter.headersec.HttpHeaderSecurityFilter;
import org.camunda.bpm.webapp.impl.security.filter.util.HttpSessionMutexListener;
import org.camunda.bpm.welcome.impl.web.bootstrap.WelcomeContainerBootstrap;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.*;
import static javax.servlet.DispatcherType.*;

/**
 * Using Micronaut Servlet with Jetty to run the REST API as a servlet.
 *
 * see https://micronaut-projects.github.io/micronaut-servlet/latest/guide/#jetty
 *
 * @author Martin Sawilla
 */
@Singleton
//Implementation based on Spring-Boot-Starter: https://github.com/camunda/camunda-bpm-spring-boot-starter/tree/master/starter-webapp-core/src/main/java/org/camunda/bpm/spring/boot/starter/webapp
public class JettyServerCustomizer implements BeanCreatedEventListener<Server> {

    private static final Logger log = LoggerFactory.getLogger(JettyServerCustomizer.class);

    protected final Configuration configuration;

    public JettyServerCustomizer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Server onCreated(BeanCreatedEvent<Server> event) {

        Server jettyServer = event.getBean();

        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        contextHandlerCollection.addHandler(jettyServer.getHandler());

        if (configuration.getRest().isEnabled()) {
            ServletContextHandler restServletContextHandler = new ServletContextHandler();
            restServletContextHandler.setContextPath(configuration.getRest().getContextPath());
            restServletContextHandler.addServlet(new ServletHolder(new ServletContainer(new RestApp())), "/*");

            if (configuration.getRest().isBasicAuthEnabled()) {
                // see https://docs.camunda.org/manual/latest/reference/rest/overview/authentication/
                FilterHolder filterHolder = new FilterHolder(ProcessEngineAuthenticationFilter.class);
                filterHolder.setInitParameter("authentication-provider", "org.camunda.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider");
                restServletContextHandler.addFilter(filterHolder, "/*", EnumSet.of(REQUEST));
                log.debug("REST API - Basic authentication enabled");
            }

            contextHandlerCollection.addHandler(restServletContextHandler);

            log.info("REST API initialized on {}/*", configuration.getRest().getContextPath());
        }

        if (configuration.getWebapps().isEnabled()) {
            ServletContextHandler webappsContextHandler = new ServletContextHandler();
            Servlet defaultServlet = new DefaultServlet();
            ServletHolder webappsHolder = new ServletHolder("webapps", defaultServlet);
            webappsContextHandler.addServlet(webappsHolder, "/*");
            webappsContextHandler.setContextPath(configuration.getWebapps().getContextPath());

            // see https://stackoverflow.com/questions/11410388/add-more-than-one-resource-directory-to-jetty
            // and https://www.eclipse.org/jetty/documentation/jetty-9/index.html#resource-handler
            Resource webappsResource = Resource.newClassPathResource("/META-INF/resources/webjars/camunda");
            Resource pluginsResource = Resource.newClassPathResource("/META-INF/resources");
            ResourceCollection resources = new ResourceCollection(webappsResource, pluginsResource);
            webappsContextHandler.setBaseResource(resources);

            webappsContextHandler.addEventListener(new CockpitContainerBootstrap());
            webappsContextHandler.addEventListener(new AdminContainerBootstrap());
            webappsContextHandler.addEventListener(new TasklistContainerBootstrap());
            webappsContextHandler.addEventListener(new WelcomeContainerBootstrap());
            webappsContextHandler.addEventListener(new HttpSessionMutexListener());
            webappsContextHandler.addEventListener(new ServletContextInitializedListener(configuration));

            webappsContextHandler.setSessionHandler(new SessionHandler());

            contextHandlerCollection.addHandler(webappsContextHandler);

            log.info("Webapps initialized on {}", configuration.getWebapps().getContextPath());
        }

        jettyServer.setHandler(contextHandlerCollection);

        return jettyServer;
    }

    /**
      * The configuration of the Camunda Webapps is called with a ServletContextListener because in
      * JettyServerCustomizer#onCreated some underlying methods (e.g. Cockpit.getRuntimeDelegate()) would result into
      * a NullPointerException.
      */
    static class ServletContextInitializedListener implements ServletContextListener {
        private static final Logger log = LoggerFactory.getLogger(ServletContextInitializedListener.class);

        protected static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(REQUEST);

        protected static ServletContext servletContext;

        protected final Configuration configuration;

        public ServletContextInitializedListener(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            servletContext = sce.getServletContext();

            servletContext.addServlet("CockpitApp", new ServletContainer(new CockpitApp())).addMapping("/api/cockpit/*");
            servletContext.addServlet("AdminApp", new ServletContainer(new AdminApp())).addMapping("/api/admin/*");
            servletContext.addServlet("TasklistApp", new ServletContainer(new TasklistApp())).addMapping("/api/tasklist/*");
            servletContext.addServlet("EngineRestApp", new ServletContainer(new EngineRestApp())).addMapping("/api/engine/*");
            servletContext.addServlet("WelcomeApp", new ServletContainer(new WelcomeApp())).addMapping("/api/welcome/*");
            registerFilter("ProcessEnginesFilter", ProcessEnginesFilter.class, "/api/*", "/app/*");
            registerFilter("AuthenticationFilter", AuthenticationFilter.class, "/api/*", "/app/*");
            registerFilter("SecurityFilter", WebappSecurityFilter.class, singletonMap("configFile", "/securityFilterRules.json"), "/api/*", "/app/*");
            registerFilter("CsrfPreventionFilter", CsrfPreventionFilter.class, getCsrfInitParams(), "/api/*", "/app/*");
            registerFilter("HttpHeaderSecurityFilter", HttpHeaderSecurityFilter.class, getHeaderSecurityInitParams(), "/api/*", "/app/*");
            registerFilter("EmptyBodyFilter", EmptyBodyFilter.class, "/api/*", "/app/*");
            registerFilter("CacheControlFilter", CacheControlFilter.class, "/api/*", "/app/*");
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {}

        protected Map<String, String> getCsrfInitParams(){
            Map<String, String> initParams = new HashMap<>();

            configuration.getWebapps().getCsrf().getTargetOrigin().ifPresent(it -> initParams.put("targetOrigin", it));
            configuration.getWebapps().getCsrf().getDenyStatus().ifPresent(it -> initParams.put("denyStatus", it.toString()));
            configuration.getWebapps().getCsrf().getRandomClass().ifPresent(it -> initParams.put("randomClass", it));
            configuration.getWebapps().getCsrf().getEntryPoints().ifPresent(it -> initParams.put("entryPoints", String.join(",", it)));
            if(configuration.getWebapps().getCsrf().getEnableSecureCookie()) {
                initParams.put("enableSecureCookie", "true");
            }
            if(configuration.getWebapps().getCsrf().getEnableSameSiteCookie()) {
                configuration.getWebapps().getCsrf().getSameSiteCookieOption().ifPresent(it -> initParams.put("sameSiteCookieOption", it));
                configuration.getWebapps().getCsrf().getSameSiteCookieValue().ifPresent(it -> initParams.put("sameSiteCookieValue", it));
            } else {
                initParams.put("enableSameSiteCookie", "false");
            }
            configuration.getWebapps().getCsrf().getCookieName().ifPresent(it -> initParams.put("cookieName", it));

            return initParams;
        }

        protected Map<String, String> getHeaderSecurityInitParams() {
            Map<String, String> initParams = new HashMap<>();

            if(configuration.getWebapps().getHeaderSecurity().isXssProtectionDisabled()){
                initParams.put("xssProtectionDisabled", "true");
            } else {
                configuration.getWebapps().getHeaderSecurity().getXssProtectionOption().ifPresent(it -> initParams.put("xssProtectionOption", it));
                configuration.getWebapps().getHeaderSecurity().getXssProtectionValue().ifPresent(it -> initParams.put("xssProtectionValue", it));
            }

            if(configuration.getWebapps().getHeaderSecurity().isContentSecurityPolicyDisabled()) {
                initParams.put("contentSecurityPolicyDisabled", "true");
            } else {
                configuration.getWebapps().getHeaderSecurity().getContentSecurityPolicyValue().ifPresent(it -> initParams.put("contentSecurityPolicyValue", it));
            }

            if(configuration.getWebapps().getHeaderSecurity().isContentTypeOptionsDisabled()) {
                initParams.put("contentTypeOptionsDisabled", "true");
            } else {
                configuration.getWebapps().getHeaderSecurity().getContentTypeOptionsValue().ifPresent(it -> initParams.put("contentTypeOptionsValue", it));
            }

            if(!configuration.getWebapps().getHeaderSecurity().isHstsDisabled()) {
                initParams.put("hstsDisabled", "false");
                configuration.getWebapps().getHeaderSecurity().getHstsMaxAge().ifPresent(it -> initParams.put("hstsMaxAge", it.toString()));
                if(!configuration.getWebapps().getHeaderSecurity().isHstsIncludeSubdomainsDisabled()) {
                    initParams.put("hstsIncludeSubdomainsDisabled", "false");
                }
                configuration.getWebapps().getHeaderSecurity().getHstsValue().ifPresent(it -> initParams.put("hstsValue", it));
            }

            return initParams;
        }

        protected void registerFilter(String filterName, Class<? extends Filter> filterClass, String... urlPatterns) {
            registerFilter(filterName, filterClass, null, urlPatterns);
        }

        protected void registerFilter(String filterName, Class<? extends Filter> filterClass, Map<String, String> initParams, String... urlPatterns) {
            FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);
            if (filterRegistration == null) {
                filterRegistration = servletContext.addFilter(filterName, filterClass);
                filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, urlPatterns);
                if (initParams != null) {
                    filterRegistration.setInitParameters(initParams);
                }
                log.debug("Filter {} for URL {} registered", filterName, urlPatterns);
            }
        }
    }
}

