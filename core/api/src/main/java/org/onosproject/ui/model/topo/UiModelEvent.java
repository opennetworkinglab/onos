/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ui.model.topo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.event.AbstractEvent;

/**
 * UI Topology model events.
 */
public class UiModelEvent extends AbstractEvent<UiModelEvent.Type, UiElement> {

    /**
     * Enumeration of event types.
     */
    public enum Type {
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

    private final ObjectNode data;
    private final String memo;

    /**
     * Creates a UI model event. Note that the memo field can be used to
     * pass a hint to the listener about the event.
     *
     * @param type    event type
     * @param subject subject of the event
     * @param data    data containing details of the subject
     * @param memo    a note about the event
     */
    public UiModelEvent(Type type, UiElement subject, ObjectNode data,
                        String memo) {
        super(type, subject);
        this.data = data;
        this.memo = memo;
    }

    /**
     * Returns the data of the subject.
     *
     * @return the subject data
     */
    public ObjectNode data() {
        return data;
    }

    /**
     * Returns the memo.
     *
     * @return the memo
     */
    public String memo() {
        return memo;
    }

}
