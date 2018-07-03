/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.api;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes vTap event.
 */
public class OpenstackVtapEvent extends AbstractEvent<OpenstackVtapEvent.Type, OpenstackVtap> {

    /**
     * Type of vTap events.
     */
    public enum Type {

        /**
         * Signifies that a new vTap has been added.
         */
        VTAP_ADDED,

        /**
         * Signifies that a vTap has been removed.
         */
        VTAP_REMOVED,

        /**
         * Signifies that a vTap data changed.
         */
        VTAP_UPDATED,
    }

    private OpenstackVtap prevSubject;

    /**
     * Creates an event of a given type and for the specified vTap and the
     * current time.
     *
     * @param type vTap event type
     * @param vTap event vTap subject
     */
    public OpenstackVtapEvent(Type type, OpenstackVtap vTap) {
        super(type, vTap);
    }

    /**
     * Creates an event of a given type and for the specified vTap and time.
     *
     * @param type vTap event type
     * @param vTap event vTap subject
     * @param time occurrence time
     */
    public OpenstackVtapEvent(Type type, OpenstackVtap vTap, long time) {
        super(type, vTap, time);
    }

    /**
     * Creates an event with previous subject.
     *
     * The previous subject is ignored if the type is not moved or updated
     *
     * @param type vTap event type
     * @param vTap event vTap subject
     * @param prevSubject previous vTap subject
     */
    public OpenstackVtapEvent(Type type, OpenstackVtap vTap, OpenstackVtap prevSubject) {
        super(type, vTap);
        if (type == Type.VTAP_UPDATED) {
            this.prevSubject = prevSubject;
        }
    }

    /**
     * Gets the previous subject in this vTap event.
     *
     * @return the previous subject, or null if previous subject is not
     *         specified.
     */
    public OpenstackVtap prevSubject() {
        return this.prevSubject;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("prevSubject", prevSubject())
                .toString();
    }
}
