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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.FlowRule;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An intent that enables to tell flow level operation.
 * This instance holds a collection of flow rules that may be executed in parallel.
 */
@Beta
public class FlowRuleIntent extends Intent {

    private final Collection<FlowRule> flowRules;
    private PathIntent.ProtectionType type;

    /**
     * Creates a flow rule intent with the specified key, flow rules to be set, and
     * required network resources.
     *
     * @param appId     application id
     * @param key       key
     * @param flowRules flow rules
     * @param resources network resources
     * @param primary   primary protection type
     * @param resourceGroup resource group for this intent
     */
    public FlowRuleIntent(ApplicationId appId, Key key, Collection<FlowRule> flowRules,
                          Collection<NetworkResource> resources, PathIntent.ProtectionType primary,
                          ResourceGroup resourceGroup) {
        super(appId, key, resources, DEFAULT_INTENT_PRIORITY, resourceGroup);
        this.flowRules = ImmutableList.copyOf(checkNotNull(flowRules));
        this.type = primary;
    }

    /**
     * Creates a flow rule intent with all the same characteristics as the given
     * one except for the flow rule type.
     *
     * @param intent original flow rule intent
     * @param type   new protection type
     */
    public FlowRuleIntent(FlowRuleIntent intent, PathIntent.ProtectionType type) {
        this(intent.appId(), intent.key(), intent.flowRules(),
              intent.resources(), type, intent.resourceGroup());
    }

    /**
     * Constructor for serializer.
     */
    protected FlowRuleIntent() {
        super();
        this.flowRules = null;
        this.type = PathIntent.ProtectionType.PRIMARY;
    }

    /**
     * Returns a collection of flow rules to be set.
     *
     * @return a collection of flow rules
     */
    public Collection<FlowRule> flowRules() {
        return flowRules;
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

    public PathIntent.ProtectionType type() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("resources", resources())
                .add("flowRule", flowRules)
                .add("resourceGroup", resourceGroup())
                .toString();
    }
}
