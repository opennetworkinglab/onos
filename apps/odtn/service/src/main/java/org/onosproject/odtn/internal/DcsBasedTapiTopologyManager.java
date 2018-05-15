/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.internal;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.d.config.DeviceResourceIds;
import org.onosproject.d.config.ResourceIds;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.slf4j.Logger;

import static org.onosproject.d.config.DeviceResourceIds.DCS_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * OSGi Component for ODTN Tapi manager application.
 */
@Component(immediate = true)
@Service
public class DcsBasedTapiTopologyManager implements TapiTopologyManager {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService dcs;

    @Activate
    public void activate() {
        initDcsIfRootNotExist();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addDevice(Device device) {
        log.info("Add device: {}", device);
    }

    @Override
    public void removeDevice(Device device) {
        log.info("Remove device: {}", device);
    }

    @Override
    public void addLink(Link link) {
        log.info("Add link: {}", link);
    }

    @Override
    public void removeLink(Link link) {
        log.info("Remove link: {}", link);
    }

    @Override
    public void addPort(Device device) {
        log.info("Add port: {}", device);
    }

    @Override
    public void removePort(Device device) {
        log.info("Remove port: {}", device);
    }

    private void initDcsIfRootNotExist() {

        log.info("read root:");
        try {
            DataNode all = dcs.readNode(ResourceIds.ROOT_ID, Filter.builder().build());
            log.info("all: {}", all);
        } catch (FailedException e) {
            // FIXME debug this issue
            log.info("nothing retrievable in DCS?");
            //e.printStackTrace(System.out);
        }
        if (!dcs.nodeExist(ResourceIds.ROOT_ID)) {
            log.info("Root node does not exist!, creating...");
            try {
                log.info("create 'root' node");
                dcs.createNode(null,
                        InnerNode.builder(DeviceResourceIds.ROOT_NAME, DCS_NAMESPACE)
                                .type(DataNode.Type.SINGLE_INSTANCE_NODE).build());
            } catch (FailedException e) {
                log.info("Failed to create root???");
                //e.printStackTrace(System.out);
            }
        }
        if (!dcs.nodeExist(ResourceIds.ROOT_ID)) {
            log.info("'root' was created without error, but still not there. WTF!");
        }
    }
}
