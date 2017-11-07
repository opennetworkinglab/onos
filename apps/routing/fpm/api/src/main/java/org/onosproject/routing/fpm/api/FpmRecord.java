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

package org.onosproject.routing.fpm.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import java.util.Objects;
import com.google.common.base.MoreObjects;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class to define a Fpm record.
 */
public class FpmRecord {

    public enum Type {
        /**
         * Signifies that record came from Dhcp Relay.
         */
        DHCP_RELAY,

        /**
         * Signifies that record came from RIP.
         */
        RIP
    }

    private IpPrefix prefix;
    private IpAddress nextHop;
    private Type type;

    public FpmRecord(IpPrefix prefix, IpAddress nextHop, Type type) {
        checkNotNull(prefix, "prefix cannot be null");
        checkNotNull(nextHop, "ipAddress cannot be null");

        this.prefix = prefix;
        this.nextHop = nextHop;
        this.type = type;
    }

    /**
     * Gets IP prefix of record.
     *
     * @return the IP prefix
     */
    public IpPrefix ipPrefix() {
        return prefix;
    }

    /**
     * Gets IP address of record.
     *
     * @return the IP address
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    /**
     * Gets type of record.
     *
     * @return the type
     */
    public Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FpmRecord)) {
            return false;
        }
        FpmRecord that = (FpmRecord) obj;
        return Objects.equals(prefix, that.prefix) &&
                Objects.equals(nextHop, that.nextHop) &&
                Objects.equals(type, that.type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("prefix", prefix)
                .add("ipAddress", nextHop)
                .add("type", type)
                .toString();
    }
}
