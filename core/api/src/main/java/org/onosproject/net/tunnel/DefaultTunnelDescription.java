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
package org.onosproject.net.tunnel;

import com.google.common.base.MoreObjects;

import org.onosproject.net.AbstractDescription;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable tunnel description entity.
 */
public class DefaultTunnelDescription extends AbstractDescription
        implements TunnelDescription {

    private final TunnelId  tunnelId;
    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Tunnel.Type type;
    private final boolean isBidirectional;

    /**
     * Creates a tunnel description using the supplied information.
     *
     * @param id          TunnelId
     * @param src         ConnectPoint source
     * @param dst         ConnectPoint destination
     * @param type        tunnel type
     * @param isBidirectional        boolean
     * @param annotations optional key/value annotations
     */
    public DefaultTunnelDescription(TunnelId id, ConnectPoint src, ConnectPoint dst,
                                  Tunnel.Type type, boolean isBidirectional,
                                  SparseAnnotations... annotations) {
        super(annotations);
        this.tunnelId = id;
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.isBidirectional = isBidirectional;
    }

    @Override
    public TunnelId id() {
        return tunnelId;
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
    public Tunnel.Type type() {
        return type;
    }

    @Override
    public boolean isBidirectional() {
        return isBidirectional;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tunnelId", id())
                .add("src", src())
                .add("dst", dst())
                .add("type", type())
                .add("isBidirectional", isBidirectional())
                .toString();
    }

}
