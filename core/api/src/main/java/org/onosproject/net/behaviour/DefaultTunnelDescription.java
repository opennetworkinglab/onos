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
package org.onosproject.net.behaviour;

import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

/**
 * Default implementation of immutable tunnel description entity.
 */
@Beta
public class DefaultTunnelDescription extends AbstractDescription
        implements TunnelDescription {

    private final TunnelEndPoint src;
    private final TunnelEndPoint dst;
    private final Type type;
    // which a tunnel match up
    // tunnel producer
    private final TunnelName tunnelName; // name of a tunnel

    /**
     * Creates a tunnel description using the supplied information.
     *
     * @param src TunnelPoint source
     * @param dst TunnelPoint destination
     * @param type tunnel type
     * @param tunnelName tunnel name
     * @param annotations optional key/value annotations
     */
    public DefaultTunnelDescription(TunnelEndPoint src,
                                    TunnelEndPoint dst, Type type,
                                    TunnelName tunnelName,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.tunnelName = tunnelName;
    }

    @Override
    public TunnelEndPoint src() {
        return src;
    }

    @Override
    public TunnelEndPoint dst() {
        return dst;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public TunnelName tunnelName() {
        return tunnelName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("type", type())
                .add("tunnelName", tunnelName())
                .toString();
    }
}
