/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.host;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes host probing event.
 */
public class HostProbingEvent extends AbstractEvent<HostProbingEvent.Type, HostProbe> {

    /**
     * Type of host probing events.
     */
    public enum Type {
        /**
         * Probe has been requested.
         */
        PROBE_REQUESTED,

        /**
         * Probe timed out but still have not reach max retry.
         */
        PROBE_TIMEOUT,

        /**
         * Probe timed out and reach max retry.
         */
        PROBE_FAIL,

        /**
         * Probe has been complete normally.
         */
        PROBE_COMPLETED
    }

    private HostProbe prevSubject;

    /**
     * Creates an event of a given type and for the specified host probe and the
     * current time.
     *
     * @param type probing host event type
     * @param subject event subject
     */
    public HostProbingEvent(Type type, HostProbe subject) {
        super(type, subject);
    }

    /**
     * Creates an event of a given type and for the specified host probe and time.
     *
     * @param type probing host event type
     * @param subject event subject
     * @param time occurrence time
     */
    public HostProbingEvent(Type type, HostProbe subject, long time) {
        super(type, subject, time);
    }

    /**
     * Creates an event with previous subject.
     * The previous subject is ignored if the type is not PROBE_TIMEOUT
     *
     * @param type host event type
     * @param subject event subject
     * @param prevSubject previous host subject
     */
    public HostProbingEvent(Type type, HostProbe subject, HostProbe prevSubject) {
        super(type, subject);
        this.prevSubject = prevSubject;
    }

    /**
     * Creates an event with previous subject and specified time.
     * The previous subject is ignored if the type is not PROBE_TIMEOUT
     *
     * @param type host event type
     * @param subject event subject
     * @param prevSubject previous host subject
     * @param time occurrence time
     */
    public HostProbingEvent(Type type, HostProbe subject, HostProbe prevSubject, long time) {
        super(type, subject, time);
        this.prevSubject = prevSubject;
    }

    /**
     * Gets the previous subject in this host probe event.
     *
     * @return the previous subject, or null if previous subject is not
     *         specified.
     */
    public HostProbe prevSubject() {
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
