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

package org.onosproject.l2lb.api;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class L2LbEvent extends AbstractEvent<L2LbEvent.Type, L2Lb> {

    private L2Lb prevSubject;

    /**
     * L2 load balancer event type.
     */
    public enum Type {
        ADDED,
        REMOVED,
        UPDATED
    }

    /**
     * Constructs a L2 load balancer event.
     *
     * @param type event type
     * @param subject current L2 load balancer information
     * @param prevSubject previous L2 load balancer information
     */
    public L2LbEvent(Type type, L2Lb subject, L2Lb prevSubject) {
        super(type, subject);
        this.prevSubject = prevSubject;
    }

    /**
     * Gets previous L2 load balancer information.
     *
     * @return previous subject
     */
    public L2Lb prevSubject() {
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

        if (!(other instanceof L2LbEvent)) {
            return false;
        }

        L2LbEvent that = (L2LbEvent) other;
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
