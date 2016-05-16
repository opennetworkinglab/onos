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
package org.onosproject.cordvtn.impl.service;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.cordconfig.access.AccessAgentConfig;
import org.onosproject.cordconfig.access.AccessAgentData;
import org.onosproject.cordvtn.api.CordVtnConfig;
import org.onosproject.cordvtn.api.Instance;
import org.onosproject.cordvtn.api.InstanceHandler;
import org.onosproject.cordvtn.impl.CordVtnInstanceHandler;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.xosclient.api.VtnService;

import java.util.Map;
import java.util.Set;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.impl.CordVtnPipeline.PRIORITY_MANAGEMENT;
import static org.onosproject.cordvtn.impl.CordVtnPipeline.TABLE_ACCESS_TYPE;

/**
 * Provides network connectivity for OLT agent instances.
 */
@Component(immediate = true)
public class OltAgentInstanceHandler extends CordVtnInstanceHandler implements InstanceHandler {

    private static final Class<AccessAgentConfig> CONFIG_CLASS = AccessAgentConfig.class;
    private ConfigFactory<DeviceId, AccessAgentConfig> configFactory =
            new ConfigFactory<DeviceId, AccessAgentConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY, CONFIG_CLASS, "accessAgent") {
                @Override
                public AccessAgentConfig createConfig() {
                    return new AccessAgentConfig();
                }
            };

    private Map<DeviceId, AccessAgentData> oltAgentData = Maps.newConcurrentMap();
    private IpPrefix mgmtIpRange = null;

    @Activate
    protected void activate() {
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn-olt", "event-handler"));
        serviceType = VtnService.ServiceType.OLT_AGENT;

        configRegistry.registerConfigFactory(configFactory);
        configListener = new InternalConfigListener();

        super.activate();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void instanceDetected(Instance instance) {
        log.info("OLT agent instance detected {}", instance);

        managementAccessRule(instance.deviceId(), true);
        // TODO implement
    }

    @Override
    public void instanceRemoved(Instance instance) {
        log.info("OLT agent instance removed {}", instance);

        if (getInstances(instance.serviceId()).isEmpty()) {
            nodeManager.completeNodes().stream().forEach(node ->
                managementAccessRule(node.intBrId(), false));
        }

        // TODO implement
    }

    private void managementAccessRule(DeviceId deviceId, boolean install) {
        // TODO remove this rule after long term management network is done
        if (mgmtIpRange != null) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(mgmtIpRange)
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.LOCAL)
                    .build();

            FlowRule flowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY_MANAGEMENT)
                    .forDevice(deviceId)
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, flowRule);
        }
    }

    private void readAccessAgentConfig() {

        Set<DeviceId> deviceSubjects = configRegistry.getSubjects(DeviceId.class, CONFIG_CLASS);
        deviceSubjects.stream().forEach(subject -> {
            AccessAgentConfig config = configRegistry.getConfig(subject, CONFIG_CLASS);
            if (config != null) {
                oltAgentData.put(subject, config.getAgent());
            }
        });
    }

    @Override
    protected void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        osAccess = config.openstackAccess();
        xosAccess = config.xosAccess();
        mgmtIpRange = config.managementIpRange();
    }

    public class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {

            switch (event.type()) {
                case CONFIG_UPDATED:
                case CONFIG_ADDED:
                    if (event.configClass().equals(CordVtnConfig.class)) {
                        readConfiguration();
                    } else if (event.configClass().equals(CONFIG_CLASS)) {
                        readAccessAgentConfig();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
