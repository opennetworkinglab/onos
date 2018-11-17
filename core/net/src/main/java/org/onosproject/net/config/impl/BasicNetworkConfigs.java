/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.config.impl;

import com.google.common.collect.ImmutableSet;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.config.BasicNetworkConfigService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.config.basics.BasicRegionConfig;
import org.onosproject.net.config.basics.BasicUiTopoLayoutConfig;
import org.onosproject.net.config.basics.DeviceAnnotationConfig;
import org.onosproject.net.config.basics.HostAnnotationConfig;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.config.basics.PortAnnotationConfig;
import org.onosproject.net.config.basics.PortDescriptionsConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.DEVICE_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.HOST_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.LAYOUT_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.LINK_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.REGION_SUBJECT_FACTORY;

/**
 * Component for registration of builtin basic network configurations.
 */
@Component(immediate = true, service = BasicNetworkConfigService.class)
public class BasicNetworkConfigs implements BasicNetworkConfigService {

    private static final String BASIC = "basic";
    private static final String INTERFACES = "interfaces";
    private static final String PORTS = "ports";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<DeviceId, BasicDeviceConfig>(DEVICE_SUBJECT_FACTORY,
                                                           BasicDeviceConfig.class,
                                                           BASIC) {
                @Override
                public BasicDeviceConfig createConfig() {
                    return new BasicDeviceConfig();
                }
            },
            new ConfigFactory<ConnectPoint, InterfaceConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                                                             InterfaceConfig.class,
                                                             INTERFACES,
                                                             true) {
                @Override
                public InterfaceConfig createConfig() {
                    return new InterfaceConfig();
                }
            },
            new ConfigFactory<HostId, BasicHostConfig>(HOST_SUBJECT_FACTORY,
                                                       BasicHostConfig.class,
                                                       BASIC) {
                @Override
                public BasicHostConfig createConfig() {
                    return new BasicHostConfig();
                }
            },
            new ConfigFactory<LinkKey, BasicLinkConfig>(LINK_SUBJECT_FACTORY,
                                                        BasicLinkConfig.class,
                                                        BasicLinkConfig.CONFIG_KEY) {
                @Override
                public BasicLinkConfig createConfig() {
                    return new BasicLinkConfig();
                }
            },
            new ConfigFactory<RegionId, BasicRegionConfig>(REGION_SUBJECT_FACTORY,
                                                           BasicRegionConfig.class,
                                                           BASIC) {
                @Override
                public BasicRegionConfig createConfig() {
                    return new BasicRegionConfig();
                }
            },
            new ConfigFactory<UiTopoLayoutId, BasicUiTopoLayoutConfig>(LAYOUT_SUBJECT_FACTORY,
                                                                       BasicUiTopoLayoutConfig.class,
                                                                       BASIC) {
                @Override
                public BasicUiTopoLayoutConfig createConfig() {
                    return new BasicUiTopoLayoutConfig();
                }
            },
            new ConfigFactory<ConnectPoint, PortAnnotationConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                                                                  PortAnnotationConfig.class,
                                                                  PortAnnotationConfig.CONFIG_KEY) {
                @Override
                public PortAnnotationConfig createConfig() {
                    return new PortAnnotationConfig();
                }
            },
            new ConfigFactory<DeviceId, PortDescriptionsConfig>(DEVICE_SUBJECT_FACTORY,
                                                                PortDescriptionsConfig.class,
                                                                PORTS) {
                @Override
                public PortDescriptionsConfig createConfig() {
                    return new PortDescriptionsConfig();
                }
            },
            new ConfigFactory<DeviceId, DeviceAnnotationConfig>(DEVICE_SUBJECT_FACTORY,
                                                                DeviceAnnotationConfig.class,
                                                                DeviceAnnotationConfig.CONFIG_KEY) {
                @Override
                public DeviceAnnotationConfig createConfig() {
                    return new DeviceAnnotationConfig();
                }
            },
            new ConfigFactory<HostId, HostAnnotationConfig>(HOST_SUBJECT_FACTORY,
                    HostAnnotationConfig.class,
                    HostAnnotationConfig.CONFIG_KEY) {
                @Override
                public HostAnnotationConfig createConfig() {
                    return new HostAnnotationConfig();
                }
            }

    );

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry registry;

    @Activate
    public void activate() {
        SubjectFactories.setCoreService(coreService);
        factories.forEach(registry::registerConfigFactory);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        factories.forEach(registry::unregisterConfigFactory);
        log.info("Stopped");
    }

}
