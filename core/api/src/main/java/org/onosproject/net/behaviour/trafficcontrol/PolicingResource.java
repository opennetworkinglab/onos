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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetworkResource;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction which encapsulates policer
 * data to be used as network resource.
 */
@Beta
public final class PolicingResource implements NetworkResource {

    // The policer id identifying this resource
    private final PolicerId policerId;
    // The connect point where the policer applies
    private final ConnectPoint connectPoint;

    public PolicingResource(PolicerId pId, ConnectPoint cP) {
        checkNotNull(pId, "Must specify a policer id");
        checkNotNull(cP, "Must specify a connect point");
        policerId = pId;
        connectPoint = cP;
    }

    /**
     * Return the policer id of this resource.
     *
     * @return the policer id
     */
    public PolicerId policerId() {
        return policerId;
    }

    /**
     * Returns the connect point of this resource.
     *
     * @return the connect point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", policerId())
                .add("connectPoint", connectPoint()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicingResource that = (PolicingResource) o;
        return Objects.equal(policerId, that.policerId) &&
                Objects.equal(connectPoint, that.connectPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(policerId, connectPoint);
    }

}
