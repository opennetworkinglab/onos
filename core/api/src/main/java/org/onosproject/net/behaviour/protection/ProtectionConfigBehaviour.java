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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.driver.HandlerBehaviour;

import com.google.common.annotations.Beta;


/**
 * Behaviour for configuring Device triggered protection mechanism.
 * <p>
 * <b>Protected transport entity model</b><br>
 * <pre>
 * {@literal
 * - ProtectedTransportEndpoint
 *    +- TransportEndpoint - working transport entity/path
 *    |
 *    +- TransportEndpoint - standby transport entity/path
 *    ⋮
 * }
 * </pre>
 * ProtectedTransportEndpoint is the entity representing the transport entity endpoint.
 * Traffic flowing into the protected transport endpoint will flow
 * through one of it's underlying TransportEndpoint, (=active transport entity).
 *
 * After successful creation of ProtectedPathEndpoint, implementation is expected
 * to advertise virtual Port corresponding to the ProtectedPathEndpoint created.
 */
@Beta
public interface ProtectionConfigBehaviour extends HandlerBehaviour {

    /**
     * Annotation key for virtual Port.
     */
    static String FINGERPRINT = "protection:fingerprint";

    // Implementation is expected to
    //  - Create logical entity representing protection group
    //  - Expose (virtual) Port via Device Subsystem
    //  - implementation of FlowRuleProvider and similar should translate
    //    request forwarding from/to virtual port to corresponding configuration
    //    stitching protection group.
    /**
     * Creates protected path endpoint.
     *
     * @param configuration {@link ProtectedTransportEndpointDescription}
     * @return {@link ConnectPoint} for the virtual Port added on success,
     * or exceptionally return {@link ProtectionException} as cause on error.
     */
    CompletableFuture<ConnectPoint>
        createProtectionEndpoint(ProtectedTransportEndpointDescription configuration);

    /**
     * Updates protected path endpoint configuration.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint
     * @param configuration {@link ProtectedTransportEndpointDescription}
     * @return {@code identifier} on success,
     * or exceptionally return {@link ProtectionException} as cause on error.
     */
    CompletableFuture<ConnectPoint>
        updateProtectionEndpoint(ConnectPoint identifier,
                                 ProtectedTransportEndpointDescription configuration);

    /**
     * Deletes protected path endpoint.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint
     * @return true if successfully removed, false otherwise.
     */
    // TODO Should we return Boolean or instead return Void and fail exceptionally?
    CompletableFuture<Boolean>
        deleteProtectionEndpoint(ConnectPoint identifier);

    /**
     * Retrieves {@link ProtectedTransportEndpointDescription}s on the Device.
     *
     * @return {@link ProtectedTransportEndpointDescription}s on the Device
     */
    CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointDescription>>
        getProtectionEndpointConfigs();

    /**
     * Retrieves {@link ProtectedTransportEndpointDescription} with specified ID.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint to retrieve
     * @return {@link ProtectedTransportEndpointDescription} found or null
     */
    default CompletableFuture<ProtectedTransportEndpointDescription>
        getProtectionEndpointConfig(ConnectPoint identifier) {
            return getProtectionEndpointConfigs().thenApply(m -> m.get(identifier));
    }

    /**
     * Retrieves {@link ProtectedTransportEndpointState}s on the Device.
     *
     * @return {@link ProtectedTransportEndpointState}s on the Device
     */
    CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointState>>
        getProtectionEndpointStates();

    /**
     * Retrieves {@link ProtectedTransportEndpointState} on the Device.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint to retrieve
     * @return {@link ProtectedTransportEndpointState} found or null
     */
    default CompletableFuture<ProtectedTransportEndpointState>
        getProtectionEndpointState(ConnectPoint identifier) {
            return getProtectionEndpointStates().thenApply(m -> m.get(identifier));
    }

    /**
     * Retrieves ProtectedTansportEndpoint information
     * (=virtual Port {@link ConnectPoint} and {@link ProtectedTransportEndpointState} pair)
     * on the Device.
     *
     * @param fingerprint of the protected path endpoint to retrieve
     * @return ProtectedTansportEndpoint information found or null
     */
    default CompletableFuture<Map.Entry<ConnectPoint, ProtectedTransportEndpointState>>
                                getProtectionEndpoint(String fingerprint) {
        return getProtectionEndpointStates()
                .thenApply(Map::entrySet)
                .thenApply(Set::stream)
                .thenApply(s ->
                    s.filter(e -> fingerprint.equals(e.getValue().description().fingerprint()))
                     .findFirst().orElse(null)
                );
    }

    /**
     * Attempts to forcibly switch to the one specified path by {@code index}.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint
     * @param index path index to switch to
     * @return Completes if request was accepted, fails exceptionally on error.
     *         Note: completion does not always assure working path has switched.
     */
    default CompletableFuture<Void> switchToForce(ConnectPoint identifier, int index) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Attempts to manually switch to the one specified path by {@code index}.
     * This operation would be rejected if the specified path is a fault path.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint
     * @param index path index to switch to
     * @return Completes if request was accepted, fails exceptionally on error.
     *         Note: completion does not always assure working path has switched.
     */
    CompletableFuture<Void> switchToManual(ConnectPoint identifier, int index);

    /**
     * Attempts to set the device to automatic protection mode.
     *
     * @param identifier {@link ConnectPoint} for the virtual Port representing
     *                     protected path endpoint
     * @return Completes if request was accepted, fails exceptionally on error.
     */
    default CompletableFuture<Void> switchToAutomatic(ConnectPoint identifier) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException());
        return future;
    }

    // TODO How to let one listen to async events? e.g., working path changed event
}
