/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.core.ApplicationId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flowobjective.Objective;

import java.util.Collection;

/**
 * Intent expressed as (and backed by) a collection of flow objectives through
 * which the intent is to be accomplished.
 */
public class FlowObjectiveIntent extends Intent {

    private final Collection<Objective> objectives;

    /**
     * Constructor for serialization.
     */
    protected FlowObjectiveIntent() {
        super();
        this.objectives = null;
    }

    /**
     * Creates a flow objective intent with the specified objectives and
     * resources.
     *
     * @param appId      application id
     * @param objectives backing flow objectives
     * @param resources  backing network resources
     */
    public FlowObjectiveIntent(ApplicationId appId,
                               Collection<Objective> objectives,
                               Collection<NetworkResource> resources) {
        this(appId, null, objectives, resources);
    }

    /**
     * Creates a flow objective intent with the specified objectives and
     * resources.
     *
     * @param appId      application id
     * @param key        intent key
     * @param objectives backing flow objectives
     * @param resources  backing network resources
     */
    public FlowObjectiveIntent(ApplicationId appId, Key key,
                               Collection<Objective> objectives,
                               Collection<NetworkResource> resources) {
        super(appId, key, resources, DEFAULT_INTENT_PRIORITY);
        this.objectives = objectives;
    }

    /**
     * Returns the collection of backing flow objectives.
     *
     * @return flow objectives
     */
    Collection<Objective> objectives() {
        return objectives;
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
                .add("objectives", objectives)
                .toString();
    }
}
