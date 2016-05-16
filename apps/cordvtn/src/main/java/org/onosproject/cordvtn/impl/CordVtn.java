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
package org.onosproject.cordvtn.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.cordvtn.api.CordVtnNode;
import org.onosproject.cordvtn.api.CordVtnService;
import org.onosproject.cordvtn.api.Instance;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.xosclient.api.VtnService;
import org.onosproject.xosclient.api.VtnServiceId;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.impl.CordVtnPipeline.*;
import static org.onosproject.net.group.DefaultGroupBucket.createSelectGroupBucket;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provisions service dependency capabilities between network services.
 */
@Component(immediate = true)
@Service
public class CordVtn extends CordVtnInstanceHandler implements CordVtnService {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Activate
    protected void activate() {
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn", "event-handler"));
        hostListener = new InternalHostListener();
        super.activate();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void createServiceDependency(VtnServiceId tServiceId, VtnServiceId pServiceId,
                                        boolean isBidirectional) {
        VtnService tService = getVtnService(tServiceId);
        VtnService pService = getVtnService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to create dependency between {} and {}",
                      tServiceId, pServiceId);
            return;
        }

        log.info("Created dependency between {} and {}", tService.name(), pService.name());
        serviceDependencyRules(tService, pService, isBidirectional, true);
    }

    @Override
    public void removeServiceDependency(VtnServiceId tServiceId, VtnServiceId pServiceId) {
        VtnService tService = getVtnService(tServiceId);
        VtnService pService = getVtnService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to remove dependency between {} and {}",
                      tServiceId, pServiceId);
            return;
        }

        log.info("Removed dependency between {} and {}", tService.name(), pService.name());
        serviceDependencyRules(tService, pService, true, false);
    }

    @Override
    public void instanceDetected(Instance instance) {
        VtnService service = getVtnService(instance.serviceId());
        if (service == null) {
            return;
        }

        // TODO get bidirectional information from XOS once XOS supports
        service.tenantServices().stream().forEach(
                tServiceId -> createServiceDependency(tServiceId, service.id(), true));
        service.providerServices().stream().forEach(
                pServiceId -> createServiceDependency(service.id(), pServiceId, true));

        updateProviderServiceInstances(service);
    }

    @Override
    public void instanceRemoved(Instance instance) {
        VtnService service = getVtnService(instance.serviceId());
        if (service == null) {
            return;
        }

        if (!service.providerServices().isEmpty()) {
            removeInstanceFromTenantService(instance, service);
        }
        if (!service.tenantServices().isEmpty()) {
            updateProviderServiceInstances(service);
        }
    }

    private void updateProviderServiceInstances(VtnService service) {
        GroupKey groupKey = getGroupKey(service.id());

        Set<DeviceId> devices = nodeManager.completeNodes().stream()
                .map(CordVtnNode::intBrId)
                .collect(Collectors.toSet());

        for (DeviceId deviceId : devices) {
            Group group = groupService.getGroup(deviceId, groupKey);
            if (group == null) {
                log.trace("No group exists for service {} in {}", service.id(), deviceId);
                continue;
            }

            List<GroupBucket> oldBuckets = group.buckets().buckets();
            List<GroupBucket> newBuckets = getServiceGroupBuckets(
                    deviceId, service.vni(), getInstances(service.id())).buckets();

            if (oldBuckets.equals(newBuckets)) {
                continue;
            }

            List<GroupBucket> bucketsToRemove = Lists.newArrayList(oldBuckets);
            bucketsToRemove.removeAll(newBuckets);
            if (!bucketsToRemove.isEmpty()) {
                groupService.removeBucketsFromGroup(
                        deviceId,
                        groupKey,
                        new GroupBuckets(bucketsToRemove),
                        groupKey, appId);
            }

            List<GroupBucket> bucketsToAdd = Lists.newArrayList(newBuckets);
            bucketsToAdd.removeAll(oldBuckets);
            if (!bucketsToAdd.isEmpty()) {
                groupService.addBucketsToGroup(
                        deviceId,
                        groupKey,
                        new GroupBuckets(bucketsToAdd),
                        groupKey, appId);
            }
        }
    }

    private void removeInstanceFromTenantService(Instance instance, VtnService service) {
        service.providerServices().stream().forEach(pServiceId -> {
            Map<DeviceId, Set<PortNumber>> inPorts = Maps.newHashMap();
            Map<DeviceId, GroupId> outGroups = Maps.newHashMap();

            inPorts.put(instance.deviceId(), Sets.newHashSet(instance.portNumber()));
            outGroups.put(instance.deviceId(), getGroupId(pServiceId, instance.deviceId()));

            inServiceRule(inPorts, outGroups, false);
        });
    }

    private void serviceDependencyRules(VtnService tService, VtnService pService,
                                       boolean isBidirectional, boolean install) {
        Map<DeviceId, GroupId> outGroups = Maps.newHashMap();
        Map<DeviceId, Set<PortNumber>> inPorts = Maps.newHashMap();

        nodeManager.completeNodes().stream().forEach(node -> {
            DeviceId deviceId = node.intBrId();
            GroupId groupId = createServiceGroup(deviceId, pService);
            outGroups.put(deviceId, groupId);

            Set<PortNumber> tServiceInstances = getInstances(tService.id())
                    .stream()
                    .filter(instance -> instance.deviceId().equals(deviceId))
                    .map(Instance::portNumber)
                    .collect(Collectors.toSet());
            inPorts.put(deviceId, tServiceInstances);
        });

        Ip4Prefix srcRange = tService.subnet().getIp4Prefix();
        Ip4Prefix dstRange = pService.subnet().getIp4Prefix();

        indirectAccessRule(srcRange, pService.serviceIp().getIp4Address(), outGroups, install);
        directAccessRule(srcRange, dstRange, install);
        if (isBidirectional) {
            directAccessRule(dstRange, srcRange, install);
        }
        inServiceRule(inPorts, outGroups, install);
    }

    private void indirectAccessRule(Ip4Prefix srcRange, Ip4Address serviceIp,
                                    Map<DeviceId, GroupId> outGroups, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcRange)
                .matchIPDst(serviceIp.toIpPrefix())
                .build();

        for (Map.Entry<DeviceId, GroupId> outGroup : outGroups.entrySet()) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .group(outGroup.getValue())
                    .build();

            FlowRule flowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY_HIGH)
                    .forDevice(outGroup.getKey())
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, flowRule);
        }
    }

    private void directAccessRule(Ip4Prefix srcRange, Ip4Prefix dstRange, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcRange)
                .matchIPDst(dstRange)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();

        nodeManager.completeNodes().stream().forEach(node -> {
            DeviceId deviceId = node.intBrId();
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY_DEFAULT)
                    .forDevice(deviceId)
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, flowRuleDirect);
        });
    }

    private void inServiceRule(Map<DeviceId, Set<PortNumber>> inPorts,
                               Map<DeviceId, GroupId> outGroups, boolean install) {
        for (Map.Entry<DeviceId, Set<PortNumber>> entry : inPorts.entrySet()) {
            Set<PortNumber> ports = entry.getValue();
            DeviceId deviceId = entry.getKey();

            GroupId groupId = outGroups.get(deviceId);
            if (groupId == null) {
                continue;
            }

            ports.stream().forEach(port -> {
                TrafficSelector selector = DefaultTrafficSelector.builder()
                        .matchInPort(port)
                        .build();

                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .group(groupId)
                        .build();

                FlowRule flowRule = DefaultFlowRule.builder()
                        .fromApp(appId)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .withPriority(PRIORITY_DEFAULT)
                        .forDevice(deviceId)
                        .forTable(TABLE_IN_SERVICE)
                        .makePermanent()
                        .build();

                pipeline.processFlowRule(install, flowRule);
            });
        }
    }

    private GroupId getGroupId(VtnServiceId serviceId, DeviceId deviceId) {
        return new DefaultGroupId(Objects.hash(serviceId, deviceId));
    }

    private GroupKey getGroupKey(VtnServiceId serviceId) {
        return new DefaultGroupKey(serviceId.id().getBytes());
    }

    private GroupId createServiceGroup(DeviceId deviceId, VtnService service) {
        GroupKey groupKey = getGroupKey(service.id());
        Group group = groupService.getGroup(deviceId, groupKey);
        GroupId groupId = getGroupId(service.id(), deviceId);

        if (group != null) {
            log.debug("Group {} is already exist in {}", service.id(), deviceId);
            return groupId;
        }

        GroupBuckets buckets = getServiceGroupBuckets(
                deviceId, service.vni(), getInstances(service.id()));
        GroupDescription groupDescription = new DefaultGroupDescription(
                deviceId,
                GroupDescription.Type.SELECT,
                buckets,
                groupKey,
                groupId.id(),
                appId);

        groupService.addGroup(groupDescription);
        return groupId;
    }

    private GroupBuckets getServiceGroupBuckets(DeviceId deviceId, long tunnelId,
                                                Set<Instance> instances) {
        List<GroupBucket> buckets = Lists.newArrayList();
        instances.stream().forEach(instance -> {
            Ip4Address tunnelIp = nodeManager.dpIp(instance.deviceId()).getIp4Address();
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

            if (deviceId.equals(instance.deviceId())) {
                tBuilder.setEthDst(instance.mac())
                        .setOutput(instance.portNumber());
            } else {
                ExtensionTreatment tunnelDst =
                        pipeline.tunnelDstTreatment(deviceId, tunnelIp);
                tBuilder.setEthDst(instance.mac())
                        .extension(tunnelDst, deviceId)
                        .setTunnelId(tunnelId)
                        .setOutput(nodeManager.tunnelPort(instance.deviceId()));
            }
            buckets.add(createSelectGroupBucket(tBuilder.build()));
        });
        return new GroupBuckets(buckets);
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!mastershipService.isLocalMaster(host.location().deviceId())) {
                // do not allow to proceed without mastership
                return;
            }

            Instance instance = Instance.of(host);
            switch (event.type()) {
                case HOST_UPDATED:
                case HOST_ADDED:
                    eventExecutor.execute(() -> instanceDetected(instance));
                    break;
                case HOST_REMOVED:
                    eventExecutor.execute(() -> instanceRemoved(instance));
                    break;
                default:
                    break;
            }
        }
    }
}
