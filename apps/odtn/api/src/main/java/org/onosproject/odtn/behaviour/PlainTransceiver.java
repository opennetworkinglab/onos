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
package org.onosproject.odtn.behaviour;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toResourceData;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.odtn.utils.openconfig.Transceiver;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Plain OpenConfig based implementation.
 */
public class PlainTransceiver extends AbstractHandlerBehaviour
        implements ConfigurableTransceiver {

    private final Logger log = getLogger(getClass());

    @Override
    public List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable) {
        DeviceId did = this.data().deviceId();
        Port port = handler().get(DeviceService.class).getPort(did, client);
        if (port == null) {
            log.warn("{} does not exist on {}", client, did);
            return Collections.emptyList();
        }
        String component = port.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(component)) {
            log.warn("{} annotation not found on {}@{}", OC_NAME, client, did);
            return Collections.emptyList();
        }
        return enable(component, enable);
    }

    @Override
    public List<CharSequence> enable(String component, boolean enable) {
        List<DataNode> nodes = Transceiver.enable(component, enable);

        ResourceId empty = ResourceId.builder().build();
        return Lists.transform(nodes,
                   node -> toCharSequence(toXmlCompositeStream(toCompositeData(toResourceData(empty, node)))));
    }

}
