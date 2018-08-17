/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.drivers.optical;

import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.DEVICE_SUBJECT_FACTORY;

import java.util.List;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.driver.optical.config.FlowTableConfig;
import org.onosproject.driver.optical.config.LambdaConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.driver.AbstractDriverLoader;
import org.onosproject.net.optical.OpticalDevice;

import com.google.common.collect.ImmutableList;

/**
 * Loader for other optical device drivers.
 */
@Component(immediate = true)
public class OpticalDriversLoader extends AbstractDriverLoader {

    // OSGI: help bundle plugin discover runtime package dependency.
    @SuppressWarnings("unused")
    private OpticalDevice optical;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry registry = new NetworkConfigRegistryAdapter();

    private final List<ConfigFactory> factories = ImmutableList.of(
         new ConfigFactory<DeviceId, FlowTableConfig>(DEVICE_SUBJECT_FACTORY,
                 FlowTableConfig.class,
                 FlowTableConfig.CONFIG_KEY) {
             @Override
             public FlowTableConfig createConfig() {
                 return new FlowTableConfig();
             }
         },
         new ConfigFactory<ConnectPoint, LambdaConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                 LambdaConfig.class,
                 LambdaConfig.CONFIG_KEY) {
             @Override
             public LambdaConfig createConfig() {
                 return new LambdaConfig();
             }
         });


    public OpticalDriversLoader() {
        super("/optical-drivers.xml");
    }

    @Activate
    @Override
    protected void activate() {
        factories.forEach(registry::registerConfigFactory);

        super.activate();
    }

    @Deactivate
    @Override
    protected void deactivate() {
        factories.forEach(registry::unregisterConfigFactory);
        super.deactivate();
    }

}
