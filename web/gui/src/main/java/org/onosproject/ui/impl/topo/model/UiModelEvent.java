/*
 *  Copyright 2016 Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo.model;

import org.onosproject.event.AbstractEvent;
import org.onosproject.ui.model.topo.UiElement;

/**
 * UI Topology model events.
 */
public class UiModelEvent extends AbstractEvent<UiModelEvent.Type, UiElement> {

    protected UiModelEvent(Type type, UiElement subject) {
        super(type, subject);
    }

    enum Type {
        CLUSTER_MEMBER_ADDED_OR_UPDATED,
        CLUSTER_MEMBER_REMOVED,

        REGION_ADDED_OR_UPDATED,
        REGION_REMOVED,

        DEVICE_ADDED_OR_UPDATED,
        DEVICE_REMOVED,

        LINK_ADDED_OR_UPDATED,
        LINK_REMOVED,

        HOST_ADDED_OR_UPDATED,
        HOST_MOVED,
        HOST_REMOVED
    }
}
