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
import static org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription.buildDescription;

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.BaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

// FIXME Move this to Protection handling Intent related package?
/**
 * Config object for protection end-point.
 * <p>
 * Contains equivalent of {@link ProtectedTransportEndpointDescription}.
 */
public class ProtectionConfig
        extends BaseConfig<DeviceId> {

    /**
     * {@value #CONFIG_KEY} : a netcfg ConfigKey for {@link ProtectionConfig}.
     */
    public static final String CONFIG_KEY = "protection";

    /**
     * JSON key for paths.
     * <p>
     * Value is list of {@link TransportEndpointDescription} in JSON.
     */
    private static final String PATHS = "paths";
    /**
     * JSON key for Peer {@link DeviceId}.
     */
    private static final String PEER = "peer";
    private static final String FINGERPRINT = "fingerprint";


    @Override
    public boolean isValid() {
        return isString(PEER, FieldPresence.MANDATORY) &&
               isString(FINGERPRINT, FieldPresence.MANDATORY) &&
               hasField(PATHS);
    }


    /**
     * Returns List of underlying transport entity endpoints in priority order.
     *
     * @return the transport entity endpoint descriptions
     */
    public List<TransportEndpointDescription> paths() {
        return getList(PATHS,
                       jsonStr -> decode(jsonStr, TransportEndpointDescription.class));
    }

    /**
     * Sets the List of underlying transport entity endpoints in priority order.
     *
     * @param paths the transport entity endpoint descriptions
     * @return self
     */
    public ProtectionConfig paths(List<TransportEndpointDescription> paths) {
        setList(PATHS,
                elm -> encode(elm, TransportEndpointDescription.class).toString(),
                paths);
        return this;
    }

    /**
     * Returns DeviceId of remote peer of this endpoint.
     *
     * @return the peer
     */
    public DeviceId peer() {
        return DeviceId.deviceId(get(PEER, ""));
    }

    /**
     * Sets the DeviceId of remote peer of this endpoint.
     *
     * @param peer DeviceId
     * @return self
     */
    public ProtectionConfig peer(DeviceId peer) {
        setOrClear(PEER, peer.toString());
        return this;
    }

    /**
     * Returns fingerprint to identify this protected transport entity.
     *
     * @return the fingerprint
     */
    public String fingerprint() {
        return get(FINGERPRINT, "");
    }

    /**
     * Sets the fingerprint to identify this protected transport entity.
     *
     * @param fingerprint the fingerprint
     * @return self
     */
    public ProtectionConfig fingerprint(String fingerprint) {
        setOrClear(FINGERPRINT, checkNotNull(fingerprint));
        return this;
    }

    /**
     * Returns equivalent of this Config as {@link ProtectedTransportEndpointDescription}.
     *
     * @return {@link ProtectedTransportEndpointDescription}
     */
    public ProtectedTransportEndpointDescription asDescription() {
        return buildDescription(paths(), peer(), fingerprint());
    }

    @Override
    public String toString() {
        return object.toString();
    }

    /**
     * Create a {@link ProtectionConfig}.
     * <p>
     * Note: created instance needs to be initialized by #init(..) before using.
     */
    public ProtectionConfig() {
        super();
    }

    /**
     * Create a {@link ProtectionConfig} for specified Device.
     * <p>
     * Note: created instance is not bound to NetworkConfigService,
     * cannot use {@link #apply()}. Must be passed to the service
     * using NetworkConfigService#applyConfig
     *
     * @param did DeviceId
     */
    public ProtectionConfig(DeviceId did) {
        ObjectMapper mapper = new ObjectMapper();
        init(did, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }
}
