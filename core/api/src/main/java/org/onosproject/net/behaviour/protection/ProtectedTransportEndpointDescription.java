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
package org.onosproject.net.behaviour.protection;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.onosproject.net.DeviceId;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * Configuration for a protected transport entity endpoint.
 */
@Beta
@Immutable
public class ProtectedTransportEndpointDescription {

    /**
     * List of underlying transport entity endpoints in priority order.
     */
    private final List<TransportEndpointDescription> paths;

    /**
     * DeviceId of remote peer of this endpoint.
     */
    private final DeviceId peer;

    // Note: Do we need opaque configuration?
    //    ⇨ For device specific configuration,
    //        NO, it should be expressed as org.onosproject.net.config.Config
    //       For information needed for caller to correlate Intent to description,
    //        use fingerprint
    /**
     * Caller specified fingerprint to identify this protected transport entity.
     * <p>
     * Virtual port corresponding to this description,
     * should have annotations {@link ProtectionConfigBehaviour#FINGERPRINT}
     * with the value specified by this field.
     */
    private final String fingerprint;


    /**
     * Constructor.
     *
     * @param paths {@link TransportEndpointDescription}s
     * @param peer remote peer of this endpoint
     * @param fingerprint to identify this protected transport entity.
     */
    protected ProtectedTransportEndpointDescription(List<TransportEndpointDescription> paths,
                                                    DeviceId peer,
                                                    String fingerprint) {
        this.paths = ImmutableList.copyOf(paths);
        this.peer = checkNotNull(peer);
        this.fingerprint = checkNotNull(fingerprint);
    }

    /**
     * Returns List of underlying transport entity endpoints in priority order.
     *
     * @return the transport entity endpoint descriptions
     */
    public List<TransportEndpointDescription> paths() {
        return paths;
    }

    /**
     * Returns DeviceId of remote peer of this endpoint.
     * @return the peer
     */
    public DeviceId peer() {
        return peer;
    }

    /**
     * Returns fingerprint to identify this protected transport entity.
     *
     * @return the fingerprint
     */
    public String fingerprint() {
        return fingerprint;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("paths", paths)
                .add("peer", peer)
                .add("fingerprint", fingerprint)
                .toString();
    }

    /**
     * Creates a {@link ProtectedTransportEndpointDescription}.
     *
     * @param paths {@link TransportEndpointDescription}s forming protection
     * @param peer DeviceId of remote peer of this endpoint.
     * @param fingerprint opaque fingerprint object. must be serializable.
     * @return {@link TransportEndpointDescription}
     */
    public static final ProtectedTransportEndpointDescription
            buildDescription(List<TransportEndpointDescription> paths,
                             DeviceId peer,
                             String fingerprint) {
        return new ProtectedTransportEndpointDescription(paths, peer, fingerprint);
    }

    /**
     * Creates a {@link ProtectedTransportEndpointDescription}.
     *
     * @param paths {@link TransportEndpointDescription}s forming protection
     * @param peer DeviceId of remote peer of this endpoint.
     * @param fingerprint opaque fingerprint object. must be serializable.
     * @return {@link TransportEndpointDescription}
     */
    public static final ProtectedTransportEndpointDescription
                            of(List<TransportEndpointDescription> paths,
                               DeviceId peer,
                               String fingerprint) {
        return new ProtectedTransportEndpointDescription(paths, peer, fingerprint);
    }
}
