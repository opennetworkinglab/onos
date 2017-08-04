/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.newoptical;

import com.google.common.annotations.Beta;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity to represent packet link realized by optical intent.
 */
@Beta
public class PacketLinkRealizedByOptical {
    private final ConnectPoint src, dst;
    private final Bandwidth bandwidth;
    // TODO should be list of Intent Key?
    private final Key realizingIntentKey;

    /**
     * Creates instance with specified parameters.
     *
     * @param src source connect point
     * @param dst destination connect point
     * @param realizingIntentKey key of Optical*Intent that realizes packet link between src and dst
     * @param bandwidth assigned bandwidth
     */
    public PacketLinkRealizedByOptical(ConnectPoint src, ConnectPoint dst,
                                       Key realizingIntentKey, Bandwidth bandwidth) {
        this.src = src;
        this.dst = dst;
        this.realizingIntentKey = realizingIntentKey;
        this.bandwidth = bandwidth;
    }

    /**
     * Creates PacketLinkRealizedByOptical instance with specified connect points and OpticalCircuitIntent.
     * Assigned bandwidth is taken from physical limit of optical link.
     *
     * @param src source connect point
     * @param dst destination connect point
     * @param intent OpticalCircuitIntent that realizes packet link between src and dst
     * @return PacketLinkRealizedByOptical instance with specified connect points and OpticalCircuitIntent
     */
    public static PacketLinkRealizedByOptical create(ConnectPoint src, ConnectPoint dst,
                                                     OpticalCircuitIntent intent) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(intent);

        long rate = intent.getSignalType().bitRate();
        return new PacketLinkRealizedByOptical(src, dst, intent.key(), Bandwidth.bps(rate));
    }

    /**
     * Creates PacketLinkRealizedByOptical instance with specified connect points and OpticalConnectivityIntent.
     * Assigned bandwidth is taken from physical limit of optical link.
     *
     * @param src source connect point
     * @param dst destination connect point
     * @param intent OpticalConnectivityIntent that realizes packet link between src and dst
     * @return PacketLinkRealizedByOptical instance with specified connect points and OpticalConnectivityIntent
     */
    public static PacketLinkRealizedByOptical create(ConnectPoint src, ConnectPoint dst,
                                                     OpticalConnectivityIntent intent) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(intent);

        long rate = intent.getSignalType().bitRate();
        return new PacketLinkRealizedByOptical(src, dst, intent.key(), Bandwidth.bps(rate));
    }

    /**
     * Returns source connect point.
     *
     * @return source connect point
     */
    public ConnectPoint src() {
        return src;
    }

    /**
     * Returns destination connect point.
     *
     * @return destination connect point
     */
    public ConnectPoint dst() {
        return dst;
    }

    /**
     * Returns assigned bandwidth.
     *
     * @return assigned bandwidth
     */
    public Bandwidth bandwidth() {
        return bandwidth;
    }

    /**
     * Returns intent key.
     *
     * @return intent key
     */
    public Key realizingIntentKey() {
        return realizingIntentKey;
    }

    /**
     * Check if packet link is between specified two connect points.
     *
     * @param src source connect point
     * @param dst destination connect point
     * @return true if this link is between src and dst.  false if not.
     */
    public boolean isBetween(ConnectPoint src, ConnectPoint dst) {
        return (this.src.equals(src) && this.dst.equals(dst));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PacketLinkRealizedByOptical that = (PacketLinkRealizedByOptical) o;

        if (!src.equals(that.src)) {
            return false;
        }
        if (!dst.equals(that.dst)) {
            return false;
        }
        if (!bandwidth.equals(that.bandwidth)) {
            return false;
        }
        return realizingIntentKey.equals(that.realizingIntentKey);

    }

    @Override
    public int hashCode() {
        int result = src.hashCode();
        result = 31 * result + dst.hashCode();
        result = 31 * result + bandwidth.hashCode();
        result = 31 * result + realizingIntentKey.hashCode();
        return result;
    }
}
