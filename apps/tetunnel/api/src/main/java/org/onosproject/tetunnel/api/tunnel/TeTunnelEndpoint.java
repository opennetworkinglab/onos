/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.tetunnel.api.tunnel;

import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import java.util.Objects;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * TE tunnel endpoint implementation.
 */
public class TeTunnelEndpoint implements TunnelEndPoint {

    private final TeNodeKey teNodeKey;
    private final TtpKey ttpKey;

    /**
     * Creates a TE tunnel end point instance with supplied information.
     *
     * @param teNodeKey key of the TE node of this end point
     * @param ttpKey key of the TE termination point of this end point
     */
    public TeTunnelEndpoint(TeNodeKey teNodeKey, TtpKey ttpKey) {
        this.teNodeKey = teNodeKey;
        this.ttpKey = ttpKey;
    }

    /**
     * Returns key of the TE node of this end point.
     *
     * @return key of corresponding TE node
     */
    public TeNodeKey teNodeKey() {
        return teNodeKey;
    }

    /**
     * Returns key of the TE termination point of this end point.
     *
     * @return key of corresponding TE termination point
     */
    public TtpKey ttpKey() {
        return ttpKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teNodeKey, ttpKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TeTunnelEndpoint) {
            final TeTunnelEndpoint other = (TeTunnelEndpoint) obj;
            return Objects.equals(this.teNodeKey, other.teNodeKey) &&
                    Objects.equals(this.ttpKey, other.ttpKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("teNodeKey", teNodeKey)
                .add("ttpKey", ttpKey)
                .toString();
    }
}
