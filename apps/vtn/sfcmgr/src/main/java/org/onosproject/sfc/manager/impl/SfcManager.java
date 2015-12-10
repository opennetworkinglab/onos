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
package org.onosproject.sfc.manager.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.onlab.util.KryoNamespace;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.NshServicePathId;
import org.onosproject.sfc.forwarder.ServiceFunctionForwarderService;
import org.onosproject.sfc.forwarder.impl.ServiceFunctionForwarderImpl;
import org.onosproject.sfc.installer.FlowClassifierInstallerService;
import org.onosproject.sfc.installer.impl.FlowClassifierInstallerImpl;
import org.onosproject.sfc.manager.NshSpiIdGenerators;
import org.onosproject.sfc.manager.SfcService;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.event.VtnRscEvent;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.service.VtnRscService;

import org.slf4j.Logger;

/**
 * Provides implementation of SFC Service.
 */
@Component(immediate = true)
@Service
public class SfcManager implements SfcService {

    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.vtn";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VtnRscService vtnRscService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    protected ApplicationId appId;
    private ServiceFunctionForwarderService serviceFunctionForwarderService;
    private FlowClassifierInstallerService flowClassifierInstallerService;

    private final VtnRscListener vtnRscListener = new InnerVtnRscListener();

    private ConcurrentMap<PortChainId, NshServicePathId> nshSpiPortChainMap = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        serviceFunctionForwarderService = new ServiceFunctionForwarderImpl(appId);
        flowClassifierInstallerService = new FlowClassifierInstallerImpl(appId);

        vtnRscService.addListener(vtnRscListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(TenantId.class)
                .register(PortPairId.class)
                .register(PortPairGroupId.class)
                .register(FlowClassifierId.class)
                .register(PortChainId.class);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vtnRscService.removeListener(vtnRscListener);

        log.info("Stopped");
    }

    /*
     * Handle events.
     */
    private class InnerVtnRscListener implements VtnRscListener {
        @Override
        public void event(VtnRscEvent event) {

            if (VtnRscEvent.Type.PORT_PAIR_PUT == event.type()) {
                PortPair portPair = ((VtnRscEventFeedback) event.subject()).portPair();
                onPortPairCreated(portPair);
            } else if (VtnRscEvent.Type.PORT_PAIR_DELETE == event.type()) {
                PortPair portPair = ((VtnRscEventFeedback) event.subject()).portPair();
                onPortPairDeleted(portPair);
            } else if (VtnRscEvent.Type.PORT_PAIR_UPDATE == event.type()) {
                PortPair portPair = ((VtnRscEventFeedback) event.subject()).portPair();
                onPortPairDeleted(portPair);
                onPortPairCreated(portPair);
            } else if (VtnRscEvent.Type.PORT_PAIR_GROUP_PUT == event.type()) {
                PortPairGroup portPairGroup = ((VtnRscEventFeedback) event.subject()).portPairGroup();
                onPortPairGroupCreated(portPairGroup);
            } else if (VtnRscEvent.Type.PORT_PAIR_GROUP_DELETE == event.type()) {
                PortPairGroup portPairGroup = ((VtnRscEventFeedback) event.subject()).portPairGroup();
                onPortPairGroupDeleted(portPairGroup);
            } else if (VtnRscEvent.Type.PORT_PAIR_GROUP_UPDATE == event.type()) {
                PortPairGroup portPairGroup = ((VtnRscEventFeedback) event.subject()).portPairGroup();
                onPortPairGroupDeleted(portPairGroup);
                onPortPairGroupCreated(portPairGroup);
            } else if (VtnRscEvent.Type.FLOW_CLASSIFIER_PUT == event.type()) {
                FlowClassifier flowClassifier = ((VtnRscEventFeedback) event.subject()).flowClassifier();
                onFlowClassifierCreated(flowClassifier);
            } else if (VtnRscEvent.Type.FLOW_CLASSIFIER_DELETE == event.type()) {
                FlowClassifier flowClassifier = ((VtnRscEventFeedback) event.subject()).flowClassifier();
                onFlowClassifierDeleted(flowClassifier);
            } else if (VtnRscEvent.Type.FLOW_CLASSIFIER_UPDATE == event.type()) {
                FlowClassifier flowClassifier = ((VtnRscEventFeedback) event.subject()).flowClassifier();
                onFlowClassifierDeleted(flowClassifier);
                onFlowClassifierCreated(flowClassifier);
            } else if (VtnRscEvent.Type.PORT_CHAIN_PUT == event.type()) {
                PortChain portChain = (PortChain) ((VtnRscEventFeedback) event.subject()).portChain();
                onPortChainCreated(portChain);
            } else if (VtnRscEvent.Type.PORT_CHAIN_DELETE == event.type()) {
                PortChain portChain = (PortChain) ((VtnRscEventFeedback) event.subject()).portChain();
                onPortChainDeleted(portChain);
            } else if (VtnRscEvent.Type.PORT_CHAIN_UPDATE == event.type()) {
                PortChain portChain = (PortChain) ((VtnRscEventFeedback) event.subject()).portChain();
                onPortChainDeleted(portChain);
                onPortChainCreated(portChain);
            }
        }
    }

    @Override
    public void onPortPairCreated(PortPair portPair) {
        log.debug("onPortPairCreated");
        // TODO: Modify forwarding rule on port-pair creation.
    }

    @Override
    public void onPortPairDeleted(PortPair portPair) {
        log.debug("onPortPairDeleted");
        // TODO: Modify forwarding rule on port-pair deletion.
    }

    @Override
    public void onPortPairGroupCreated(PortPairGroup portPairGroup) {
        log.debug("onPortPairGroupCreated");
        // TODO: Modify forwarding rule on port-pair-group creation.
    }

    @Override
    public void onPortPairGroupDeleted(PortPairGroup portPairGroup) {
        log.debug("onPortPairGroupDeleted");
        // TODO: Modify forwarding rule on port-pair-group deletion.
    }

    @Override
    public void onFlowClassifierCreated(FlowClassifier flowClassifier) {
        log.debug("onFlowClassifierCreated");
        // TODO: Modify forwarding rule on flow-classifier creation.
    }

    @Override
    public void onFlowClassifierDeleted(FlowClassifier flowClassifier) {
        log.debug("onFlowClassifierDeleted");
        // TODO: Modify forwarding rule on flow-classifier deletion.
    }

    @Override
    public void onPortChainCreated(PortChain portChain) {
        NshServicePathId nshSPI;
        log.info("onPortChainCreated");
        if (nshSpiPortChainMap.containsKey(portChain.portChainId())) {
            nshSPI = nshSpiPortChainMap.get(portChain.portChainId());
        } else {
            nshSPI = NshServicePathId.of(NshSpiIdGenerators.create());
            nshSpiPortChainMap.put(portChain.portChainId(), nshSPI);
        }

        // install in OVS.
        flowClassifierInstallerService.installFlowClassifier(portChain, nshSPI);
        serviceFunctionForwarderService.installForwardingRule(portChain, nshSPI);
    }

    @Override
    public void onPortChainDeleted(PortChain portChain) {
        log.info("onPortChainDeleted");
        if (!nshSpiPortChainMap.containsKey(portChain.portChainId())) {
            throw new ItemNotFoundException("Unable to find NSH SPI");
        }

        NshServicePathId nshSPI = nshSpiPortChainMap.get(portChain.portChainId());
        // uninstall from OVS.
        flowClassifierInstallerService.unInstallFlowClassifier(portChain, nshSPI);
        serviceFunctionForwarderService.unInstallForwardingRule(portChain, nshSPI);

        // remove SPI. No longer it will be used.
        nshSpiPortChainMap.remove(nshSPI);
    }
}
