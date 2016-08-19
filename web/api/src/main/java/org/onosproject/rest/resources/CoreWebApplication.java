/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.rest.resources;

import org.onlab.rest.AbstractWebApplication;
import org.onosproject.rest.exceptions.InvalidConfigExceptionMapper;

import java.util.Set;

/**
 * Core REST APIs web application.
 */
public class CoreWebApplication extends AbstractWebApplication {

    @Override
    public Set<Class<?>> getClasses() {
        return getClasses(ApiDocResource.class,
                ApplicationsWebResource.class,
                ComponentConfigWebResource.class,
                NetworkConfigWebResource.class,
                ClusterWebResource.class,
                DevicesWebResource.class,
                LinksWebResource.class,
                HostsWebResource.class,
                IntentsWebResource.class,
                FlowsWebResource.class,
                GroupsWebResource.class,
                MetersWebResource.class,
                TopologyWebResource.class,
                PathsWebResource.class,
                StatisticsWebResource.class,
                MetricsWebResource.class,
                FlowObjectiveWebResource.class,
                MulticastRouteWebResource.class,
                DeviceKeyWebResource.class,
                RegionsWebResource.class,
                TenantWebResource.class,
                VirtualNetworkWebResource.class,
                MastershipWebResource.class,
                InvalidConfigExceptionMapper.class,
                DpisWebResource.class
        );
    }
}
