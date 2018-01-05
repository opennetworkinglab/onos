/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flowobjective.Objective;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Intent expressed as (and backed by) a collection of flow objectives through
 * which the intent is to be accomplished.
 */
public final class FlowObjectiveIntent extends Intent {

    private final List<Objective> objectives;
    private final List<DeviceId> devices;

    /**
     * Constructor for serialization.
     */
    protected FlowObjectiveIntent() {
        super();
        this.objectives = null;
        this.devices = null;
    }

    /**
     * Creates a flow objective intent with the specified objectives and
     * resources.
     *
     * @param appId      application id
     * @param key        intent key
     * @param devices    list of target devices; in same order as the objectives
     * @param objectives backing flow objectives
     * @param resources  backing network resources
     * @param resourceGroup resource goup for this intent
     */
    public FlowObjectiveIntent(ApplicationId appId,
                               Key key,
                               List<DeviceId> devices,
                               List<Objective> objectives,
                               Collection<NetworkResource> resources,
                               ResourceGroup resourceGroup) {
        super(appId, key, resources, DEFAULT_INTENT_PRIORITY, resourceGroup);
        checkArgument(devices.size() == objectives.size(),
                      "Number of devices and objectives does not match");
        this.objectives = ImmutableList.copyOf(objectives);
        this.devices = ImmutableList.copyOf(devices);
    }

    /**
     * Returns the collection of backing flow objectives.
     *
     * @return flow objectives
     */
    public List<Objective> objectives() {
        return objectives;
    }

    /**
     * Returns the list of devices for the flow objectives.
     *
     * @return devices
     */
    public List<DeviceId> devices() {
        return devices;
    }


    @Override
    public boolean isInstallable() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("resources", resources())
                .add("device", devices())
                .add("objectives", objectives())
                .add("resourceGroup", resourceGroup())
                .toString();
    }
}
