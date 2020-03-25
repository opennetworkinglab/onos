/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Provides RouteRefreshCapabilityTlv.
 */
public class RouteRefreshCapabilityTlv implements BgpValueType {

    /*
       Capability Code = 2
       Length = 0

       REFERENCE : RFC 2918
     */
    private static final Logger log = LoggerFactory.getLogger(RouteRefreshCapabilityTlv.class);

    public static final byte TYPE = 2;
    public static final byte LENGTH = 0;

    private boolean isSupported = false;

    /**
     * Constructor to initialize variables.
     * @param isSupported Is Route Refresh supported
     */
    public RouteRefreshCapabilityTlv(boolean isSupported) {
        this.isSupported = isSupported;
    }

    /**
     * Returns object of RouteRefreshCapabilityTlv.
     * @param isSupported Is Route Refresh supported
     * @return object of RouteRefreshCapabilityTlv
     */
    public static RouteRefreshCapabilityTlv of(boolean isSupported) {
        return new RouteRefreshCapabilityTlv(isSupported);
    }

    /**
     * Returns isSupported value.
     * @return isSupported value
     */
    public boolean isSupported() {
        return isSupported;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSupported);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RouteRefreshCapabilityTlv) {
            RouteRefreshCapabilityTlv other = (RouteRefreshCapabilityTlv) obj;
            return Objects.equals(this.isSupported, other.isSupported);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(TYPE);
        cb.writeByte(LENGTH);

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and returns object of RouteRefreshCapabilityTlv.
     * @param cb of type channel buffer
     * @return object of RouteRefreshCapabilityTlv
     */
    public static BgpValueType read(ChannelBuffer cb) {
        //No need to read. If TLV is received, it means it is supported

        return new RouteRefreshCapabilityTlv(true);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH).toString();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
