/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.mcast;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onlab.packet.IpPrefix;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An entity representing a multicast route consisting of a source
 * and a multicast group address.
 */
@Beta
public class McastRoute {

    public enum Type {
        /**
         * Route originates from PIM.
         */
        PIM,

        /**
         * Route originates from IGMP.
         */
        IGMP,

        /**
         * Route originates from other config (ie. REST, CLI).
         */
        STATIC
    }

    private final IpPrefix source;
    private final IpPrefix group;
    private final Type type;

    public McastRoute(IpPrefix source, IpPrefix group, Type type) {
        checkNotNull(source, "Multicast route must have a source");
        checkNotNull(group, "Multicast route must specify a group address");
        checkNotNull(type, "Must indicate what type of route");
        this.source = source;
        this.group = group;
        this.type = type;
    }

    /**
     * Fetches the source address of this route.
     *
     * @return an ip address
     */
    public IpPrefix source() {
        return source;
    }

    /**
     * Fetches the group address of this route.
     *
     * @return an ip address
     */
    public IpPrefix group() {
        return group;
    }

    /**
     * Obtains how this route was created.
     * @return a type of route

     */
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("source", source)
                .add("group", group)
                .add("origin", type)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        McastRoute that = (McastRoute) o;
        return Objects.equal(source, that.source) &&
                Objects.equal(group, that.group) &&
                Objects.equal(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(source, group, type);
    }

}
