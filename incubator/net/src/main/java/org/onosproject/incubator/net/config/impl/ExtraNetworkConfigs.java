/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.incubator.net.config.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.net.domain.IntentDomainConfig;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.onosproject.incubator.net.config.basics.ExtraSubjectFactories.INTENT_DOMAIN_SUBJECT_FACTORY;

/**
 * Component for registration of builtin basic network configurations.
 */
@Component(immediate = true)
public class ExtraNetworkConfigs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<IntentDomainId, IntentDomainConfig>(INTENT_DOMAIN_SUBJECT_FACTORY,
                                                                  IntentDomainConfig.class,
                                                                  "basic") {
                @Override
                public IntentDomainConfig createConfig() {
                    return new IntentDomainConfig();
                }
            }
    );

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Activate
    public void activate() {
        factories.forEach(registry::registerConfigFactory);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        factories.forEach(registry::unregisterConfigFactory);
        log.info("Stopped");
    }

}
