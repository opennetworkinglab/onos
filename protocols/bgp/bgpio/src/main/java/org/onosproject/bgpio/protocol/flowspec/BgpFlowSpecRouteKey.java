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

package org.onosproject.bgpio.protocol.flowspec;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides BGP flow specification route index.
 */
public class BgpFlowSpecRouteKey implements Comparable<Object> {

    private static final Logger log = LoggerFactory.getLogger(BgpFlowSpecRouteKey.class);

    private final String routeKey;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeKey route key
     */
    public BgpFlowSpecRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    /**
     * Returns route key.
     *
     * @return route key
     */
    public String routeKey() {
        return this.routeKey;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(routeKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFlowSpecRouteKey) {
            BgpFlowSpecRouteKey other = (BgpFlowSpecRouteKey) obj;
            return this.routeKey.equals(other.routeKey);
        }
        return false;
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }

        if (o instanceof BgpFlowSpecRouteKey) {
            BgpFlowSpecRouteKey other = (BgpFlowSpecRouteKey) o;
            if (this.routeKey.compareTo(other.routeKey) != 0) {
                return this.routeKey.compareTo(other.routeKey);
            }
            return 0;
        }
        return 1;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("routeKey", routeKey)
                .toString();
    }
}
