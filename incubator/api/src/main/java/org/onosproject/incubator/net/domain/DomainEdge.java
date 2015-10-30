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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onlab.graph.AbstractEdge;
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

/**
 * Representation of a connection between an intent domain and a device. This
 * must happen using a connect point that is part of both the domain and the
 * device.
 */
@Beta
public class DomainEdge extends AbstractEdge<DomainVertex> {

    ConnectPoint connectPoint;

    public DomainEdge(DomainVertex src, DomainVertex dst, ConnectPoint connectPoint) {
        super(src, dst);
        this.connectPoint = connectPoint;
    }

    @Override
    public int hashCode() {
        return 43 * super.hashCode() + connectPoint.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DomainEdge) {
            final DomainEdge other = (DomainEdge) obj;
            return super.equals(other) &&
                    Objects.equals(this.connectPoint, other.connectPoint);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("connectPoint", connectPoint)
                .toString();
    }

    /**
     * Returns the connect point associated with the domain edge.
     *
     * @return this edges connect point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }
}
