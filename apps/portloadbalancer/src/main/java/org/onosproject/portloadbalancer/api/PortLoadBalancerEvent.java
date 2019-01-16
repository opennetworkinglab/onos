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

package org.onosproject.portloadbalancer.api;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Port load balancer event.
 */
public class PortLoadBalancerEvent extends AbstractEvent<PortLoadBalancerEvent.Type, PortLoadBalancerData> {

    private PortLoadBalancerData prevSubject;

    /**
     * Port load balancer event type.
     */
    public enum Type {
        /**
         * Port load balancer creation is requested.
         */
        ADDED,

        /**
         * Port load balancer deletion is requested.
         */
        REMOVED,

        /**
         * Port load balancer update is requested.
         * E.g. member change.
         */
        UPDATED,

        /**
         * Port load balancer creation/update is completed successfully.
         */
        INSTALLED,

        /**
         * Port load balancer deletion is completed successfully.
         */
        UNINSTALLED,

        /**
         * Error occurs during creation/update/deletion of a port load balancer.
         */
        FAILED
    }

    /**
     * Constructs a port load balancer event.
     *
     * @param type event type
     * @param subject current port load balancer information
     * @param prevSubject previous port load balancer information
     */
    public PortLoadBalancerEvent(Type type, PortLoadBalancerData subject, PortLoadBalancerData prevSubject) {
        super(type, subject);
        this.prevSubject = prevSubject;
    }

    /**
     * Gets previous port load balancer information.
     *
     * @return previous subject
     */
    public PortLoadBalancerData prevSubject() {
        return prevSubject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject(), time(), prevSubject);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PortLoadBalancerEvent)) {
            return false;
        }

        PortLoadBalancerEvent that = (PortLoadBalancerEvent) other;
        return Objects.equals(this.subject(), that.subject()) &&
                Objects.equals(this.type(), that.type()) &&
                Objects.equals(this.prevSubject, that.prevSubject);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type())
                .add("subject", subject())
                .add("prevSubject", prevSubject)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .toString();
    }
}
