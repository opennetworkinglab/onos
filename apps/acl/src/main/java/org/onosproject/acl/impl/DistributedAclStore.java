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
 *
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li, Heng Qi and Haisheng Yu
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */
package org.onosproject.acl.impl;

import com.google.common.collect.Collections2;
import org.onosproject.acl.AclRule;
import org.onosproject.acl.AclStore;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.acl.RuleId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the ACL store service.
 */
@Component(immediate = true)
@Service
public class DistributedAclStore extends AbstractStore implements AclStore {

    private final Logger log = getLogger(getClass());
    private final int defaultFlowMaxPriority = 30000;

    private ConsistentMap<RuleId, AclRule> ruleSet;
    private ConsistentMap<DeviceId, Integer> deviceToPriority;
    private ConsistentMap<RuleId, Set<DeviceId>> ruleToDevice;
    private ConsistentMap<RuleId, Set<FlowRule>> ruleToFlow;
    private ConsistentMap<RuleId, List<RuleId>> denyRuleToAllowRule;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        ApplicationId appId = coreService.getAppId("org.onosproject.acl");

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(AclRule.class)
                .register(AclRule.Action.class)
                .register(RuleId.class);

        ruleSet = storageService.<RuleId, AclRule>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("acl-rule-set")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        deviceToPriority = storageService.<DeviceId, Integer>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("device-to-priority")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        ruleToFlow = storageService.<RuleId, Set<FlowRule>>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("rule-to-flow")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        denyRuleToAllowRule = storageService.<RuleId, List<RuleId>>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("deny-to-allow")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        ruleToDevice = storageService.<RuleId, Set<DeviceId>>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("rule-to-device")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactive() {
        log.info("Stopped");
    }

    @Override
    public List<AclRule> getAclRules() {
        List<AclRule> aclRules = new ArrayList<>();
        aclRules.addAll(Collections2.transform(ruleSet.values(), Versioned::value));
        return aclRules;
    }

    @Override
    public void addAclRule(AclRule rule) {
        ruleSet.putIfAbsent(rule.id(), rule);
    }

    @Override
    public AclRule getAclRule(RuleId ruleId) {
        Versioned<AclRule> rule = ruleSet.get(ruleId);
        if (rule != null) {
            return rule.value();
        } else {
            return null;
        }
    }

    @Override
    public void removeAclRule(RuleId ruleId) {
        ruleSet.remove(ruleId);
    }

    @Override
    public void clearAcl() {
        ruleSet.clear();
        deviceToPriority.clear();
        ruleToFlow.clear();
        denyRuleToAllowRule.clear();
        ruleToDevice.clear();
    }

    @Override
    public int getPriorityByDevice(DeviceId deviceId) {
        return deviceToPriority.compute(deviceId,
                                        (id, priority) -> (priority == null) ? defaultFlowMaxPriority : (priority - 1))
                .value();
    }

    @Override
    public Set<FlowRule> getFlowByRule(RuleId ruleId) {
        Versioned<Set<FlowRule>> flowRuleSet = ruleToFlow.get(ruleId);
        if (flowRuleSet != null) {
            return flowRuleSet.value();
        } else {
            return null;
        }
    }

    @Override
    public void addRuleToFlowMapping(RuleId ruleId, FlowRule flowRule) {
        ruleToFlow.computeIf(ruleId,
                             flowRuleSet -> (flowRuleSet == null || !flowRuleSet.contains(flowRule)),
                             (id, flowRuleSet) -> {
                                 Set<FlowRule> newSet = new HashSet<>();
                                 if (flowRuleSet != null) {
                                     newSet.addAll(flowRuleSet);
                                 }
                                 newSet.add(flowRule);
                                 return newSet;
                             });
    }

    @Override
    public void removeRuleToFlowMapping(RuleId ruleId) {
        ruleToFlow.remove(ruleId);
    }

    @Override
    public List<RuleId> getAllowingRuleByDenyingRule(RuleId denyingRuleId) {
        Versioned<List<RuleId>> allowRuleIdSet = denyRuleToAllowRule.get(denyingRuleId);
        if (allowRuleIdSet != null) {
            return allowRuleIdSet.value();
        } else {
            return null;
        }
    }

    @Override
    public void addDenyToAllowMapping(RuleId denyingRuleId, RuleId allowingRuleId) {
        denyRuleToAllowRule.computeIf(denyingRuleId,
                                      ruleIdList -> (ruleIdList == null || !ruleIdList.contains(allowingRuleId)),
                                      (id, ruleIdList) -> {
                                          ArrayList<RuleId> newList = new ArrayList<>();
                                          if (ruleIdList != null) {
                                              newList.addAll(ruleIdList);
                                          }
                                          newList.add(allowingRuleId);
                                          return newList;
                                      });
    }

    @Override
    public void removeDenyToAllowMapping(RuleId denyingRuleId) {
        denyRuleToAllowRule.remove(denyingRuleId);
    }

    @Override
    public boolean checkIfRuleWorksInDevice(RuleId ruleId, DeviceId deviceId) {
        return ruleToDevice.containsKey(ruleId) && ruleToDevice.get(ruleId).value().contains(deviceId);
    }

    @Override
    public void addRuleToDeviceMapping(RuleId ruleId, DeviceId deviceId) {
        ruleToDevice.computeIf(ruleId,
                               deviceIdSet -> (deviceIdSet == null || !deviceIdSet.contains(deviceId)),
                               (id, deviceIdSet) -> {
                                   Set<DeviceId> newSet = new HashSet<>();
                                   if (deviceIdSet != null) {
                                       newSet.addAll(deviceIdSet);
                                   }
                                   newSet.add(deviceId);
                                   return newSet;
                               });
    }

    @Override
    public void removeRuleToDeviceMapping(RuleId ruleId) {
        ruleToDevice.remove(ruleId);
    }

}
