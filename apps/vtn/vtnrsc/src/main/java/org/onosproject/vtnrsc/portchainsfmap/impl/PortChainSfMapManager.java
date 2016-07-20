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
package org.onosproject.vtnrsc.portchainsfmap.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.ListIterator;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.ServiceFunctionGroup;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portchainsfmap.PortChainSfMapService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Provides implementation of the PortChainSfMapService.
 * A port pair group is nothing but group of similar service functions.
 * A port pair is nothing but a service function.
 */
@Component(immediate = true)
@Service
public class PortChainSfMapManager implements PortChainSfMapService {

    private static final String PORT_CHAIN_ID_NULL = "PortChain ID cannot be null";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortChainService portChainService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairGroupService portPairGroupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairService portPairService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean exists(PortChainId portChainId) {
        checkNotNull(portChainId, PORT_CHAIN_ID_NULL);
        return portChainService.exists(portChainId);
    }

    @Override
    public List<ServiceFunctionGroup> getServiceFunctions(PortChainId portChainId) {
        List<ServiceFunctionGroup> serviceFunctionGroupList = Lists.newArrayList();
        PortChain portChain = portChainService.getPortChain(portChainId);
        // Go through the port pair group list
        List<PortPairGroupId> portPairGrpList = portChain.portPairGroups();
        ListIterator<PortPairGroupId> listGrpIterator = portPairGrpList.listIterator();

        while (listGrpIterator.hasNext()) {
            PortPairGroupId portPairGroupId = listGrpIterator.next();
            PortPairGroup portPairGroup = portPairGroupService.getPortPairGroup(portPairGroupId);
            ServiceFunctionGroup sfg = new ServiceFunctionGroup(portPairGroup.name(), portPairGroup.description(),
                                                                portPairGroup.portPairLoadMap());
            serviceFunctionGroupList.add(sfg);
        }
        return ImmutableList.copyOf(serviceFunctionGroupList);
    }
}
