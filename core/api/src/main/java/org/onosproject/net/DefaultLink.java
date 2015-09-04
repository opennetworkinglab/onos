/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net;

import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.Link.State.ACTIVE;

/**
 * Default infrastructure link model implementation.
 */
public class DefaultLink extends AbstractModel implements Link {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Type type;
    private final State state;
    private final boolean isDurable;
    private int weight;

    /**
     * Creates an active infrastructure link using the supplied information.
     *
     * @param providerId  provider identity
     * @param src         link source
     * @param dst         link destination
     * @param type        link type
     * @param annotations optional key/value annotations
     */
    public DefaultLink(ProviderId providerId, ConnectPoint src, ConnectPoint dst,
                       Type type, Annotations... annotations) {
        this(providerId, src, dst, type, ACTIVE, false, annotations);
        this.weight = 1;
    }

    /**
     * Creates an infrastructure link using the supplied information.
     * Links marked as durable will remain in the inventory when a vanish
     * message is received and instead will be marked as inactive.
     *
     * @param providerId  provider identity
     * @param src         link source
     * @param dst         link destination
     * @param type        link type
     * @param state       link state
     * @param isDurable   indicates if the link is to be considered durable
     * @param annotations optional key/value annotations
     */
    public DefaultLink(ProviderId providerId, ConnectPoint src, ConnectPoint dst,
                       Type type, State state,
                       boolean isDurable, Annotations... annotations) {
        super(providerId, annotations);
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.state = state;
        this.isDurable = isDurable;
        this.weight = 1;
    }

    @Override
    public ConnectPoint src() {
        return src;
    }

    @Override
    public ConnectPoint dst() {
        return dst;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public boolean isDurable() {
        return isDurable;
    }

    // Note: Durability & state are purposefully omitted form equality & hashCode.

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, type);
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultLink) {
            final DefaultLink other = (DefaultLink) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .add("type", type)
                .add("state", state)
                .add("durable", isDurable)
                .add("weight", weight)
                .toString();
    }

}
