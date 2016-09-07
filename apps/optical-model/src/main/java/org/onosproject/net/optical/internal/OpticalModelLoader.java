/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.optical.internal;

import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.PortConfigOperatorRegistry;
import org.onosproject.net.optical.config.OpticalPortConfig;
import org.onosproject.net.optical.config.OpticalPortOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader which registers optical model related config, etc.
 */
@Component(immediate = true)
public class OpticalModelLoader {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortConfigOperatorRegistry portOperatorRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netcfgRegistry;


    private OpticalPortOperator opticalPortOp;

    private ConfigFactory<ConnectPoint, OpticalPortConfig>
        opticalPortConfigFactory = new ConfigFactory<ConnectPoint, OpticalPortConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                                                       OpticalPortConfig.class,
                                                       OpticalPortConfig.CONFIG_KEY) {
        @Override
        public OpticalPortConfig createConfig() {
            return new OpticalPortConfig();
        }
    };

    @Activate
    protected void activate() {
        netcfgRegistry.registerConfigFactory(opticalPortConfigFactory);

        opticalPortOp = new OpticalPortOperator();
        portOperatorRegistry.registerPortConfigOperator(opticalPortOp,
                                                        OpticalPortConfig.class);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        portOperatorRegistry.unregisterPortConfigOperator(opticalPortOp);

        netcfgRegistry.unregisterConfigFactory(opticalPortConfigFactory);
        log.info("Stopped");
    }
}
