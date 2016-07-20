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
package org.onosproject.vtnrsc.portpairgroup;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.PortPairGroup;

/**
 * Describes network Port-Pair-Group event.
 */
public class PortPairGroupEvent extends AbstractEvent<PortPairGroupEvent.Type, PortPairGroup> {
    /**
     * Type of port-pair-group events.
     */
    public enum Type {
        /**
         * Signifies that port-pair-group has been created.
         */
        PORT_PAIR_GROUP_PUT,
        /**
         * Signifies that port-pair-group has been deleted.
         */
        PORT_PAIR_GROUP_DELETE,
        /**
         * Signifies that port-pair-group has been updated.
         */
        PORT_PAIR_GROUP_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified Port-Pair-Group.
     *
     * @param type Port-Pair-Group event type
     * @param portPairGroup Port-Pair-Group subject
     */
    public PortPairGroupEvent(Type type, PortPairGroup portPairGroup) {
        super(type, portPairGroup);
    }

    /**
     * Creates an event of a given type and for the specified Port-Pair-Group.
     *
     * @param type Port-Pair-Group event type
     * @param portPairGroup Port-Pair-Group subject
     * @param time occurrence time
     */
    public PortPairGroupEvent(Type type, PortPairGroup portPairGroup, long time) {
        super(type, portPairGroup, time);
    }
}
