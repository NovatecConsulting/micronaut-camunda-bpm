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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.camunda.bpm.engine.ManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Scanner;

/**
 * @author Martin Sawilla
 */
// Implementation based on https://github.com/camunda/camunda-bpm-spring-boot-starter/blob/master/starter/src/main/java/org/camunda/bpm/spring/boot/starter/configuration/impl/custom/EnterLicenseKeyConfiguration.java
@Singleton
@Requires(property = "camunda.license-file")
public class LicenseKeyConfiguration implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger log = LoggerFactory.getLogger(LicenseKeyConfiguration.class);
    protected static final String DEFAULT_LICENSE_FILE = "camunda-license.txt";
    protected static final String HEADER_FOOTER_REGEX = "(?i)[-\\s]*(BEGIN|END)\\s*(OPTIMIZE|CAMUNDA|CAMUNDA\\s*BPM)\\s*LICENSE\\s*KEY[-\\s]*";

    protected final Configuration configuration;

    protected final ManagementService managementService;

    protected final CamundaVersion camundaVersion;

    protected String licenseKey;

    public LicenseKeyConfiguration(Configuration configuration, ManagementService managementService, CamundaVersion camundaVersion) {
        this.configuration = configuration;
        this.managementService = managementService;
        this.camundaVersion = camundaVersion;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {

        if(!camundaVersion.isEnterprise()) {
            log.warn("You are not using the Camunda Enterprise Edition dependencies. The license is not needed and will be ignored.");
            return;
        }

        // Check if there is already a license key in the database
        if(managementService.getLicenseKey() != null) {
            log.info("A license key is already registered and will be used. Please use the Camunda Cockpit to update it.");
            return;
        }

        // User provides an URL to a license-file
        if(configuration.getLicenseFile().isPresent()) {
            licenseKey = readLicenseKeyFromUrl(configuration.getLicenseFile().get());
        } else { // Otherwise check the resource folder for "camunda-license.txt"
            licenseKey = readLicenseKeyFromUrl(LicenseKeyConfiguration.class.getClassLoader().getResource(DEFAULT_LICENSE_FILE));
        }

        if(licenseKey != null) {
            managementService.setLicenseKey(licenseKey);
            log.info("Registered new license key");
        } else {
            log.warn("Could not locate the referenced license key. The license can be registered in the Camunda Cockpit.");
        }
    }

    protected String readLicenseKeyFromUrl(URL licenseFileUrl) {
        try {
            Scanner scanner = new Scanner(licenseFileUrl.openStream(), "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()){
                return scanner.next()
                        .replaceAll(HEADER_FOOTER_REGEX, "")
                        .replaceAll("\\n", "")
                        .trim();
            } else {
                return null;
            }

        } catch (Exception e) {
            log.warn("Ignoring license file {}. Details: {}", licenseFileUrl, e.getMessage());
            return null;
        }
    }

}
