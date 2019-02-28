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

package org.onosproject.net.device;

import com.google.common.annotations.Beta;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.driver.DeviceConnect;
import org.onosproject.net.provider.ProviderId;

import java.util.concurrent.CompletableFuture;

/**
 * Behavior to test device's reachability and change the mastership role on that
 * device.
 */
@Beta
public interface DeviceHandshaker extends DeviceConnect {

    /**
     * Returns true if this node is presumed to be able to send messages and
     * receive replies from the device.
     * <p>
     * The implementation should not make any attempt at actively probing the
     * device over the network, as such it should not block execution. Instead,
     * it should return a result based solely on internal state (e.g. socket
     * state). If it returns true, then this node is expected to communicate
     * with the server successfully. In other words, if any message would be
     * sent to the device immediately after this method is called and returns
     * true, then such message is expected, but NOT guaranteed, to reach the
     * device. If false, it means communication with the device is unlikely to
     * happen soon.
     * <p>
     * Some implementations might require a connection to be created via {@link
     * #connect()} before checking for reachability. Similarly, after invoking
     * {@link #disconnect()}, this method might always return false.
     *
     * @return true if the device is deemed reachable, false otherwise
     */
    boolean isReachable();

    /**
     * Similar to {@link #isReachable()}, but performs probing of the device
     * over the network. This method should be called if {@link #isReachable()}
     * returns false and the caller wants to be sure this is not a transient
     * failure state by actively probing the device.
     *
     * @return completable future eventually true if device responded to probe,
     * false otherwise
     */
    CompletableFuture<Boolean> probeReachability();

    /**
     * Checks the availability of the device. Availability denotes whether the
     * device is reachable and able to perform its functions as expected (e.g.,
     * forward traffic). Similar to {@link #isReachable()}, implementations are
     * not allowed to probe the device over the network, but the result should
     * be based solely on internal state.
     * <p>
     * Implementation of this method is optional. If not supported, an exception
     * should be thrown.
     *
     * @return true if the device is deemed available, false otherwise
     * @throws UnsupportedOperationException if this method is not supported and
     *                                       {@link #probeAvailability()} should
     *                                       be used instead.
     */
    boolean isAvailable();

    /**
     * Similar to {@link #isAvailable()} but allows probing the device over the
     * network. Differently from {@link #isAvailable()}, implementation of this
     * method is mandatory.
     *
     * @return completable future eventually true if available, false otherwise
     */
    CompletableFuture<Boolean> probeAvailability();

    /**
     * Notifies the device a mastership role change as decided by the core. The
     * implementation of this method should trigger a {@link DeviceAgentEvent}
     * signaling the mastership role accepted by the device.
     *
     * @param newRole new mastership role
     * @throws UnsupportedOperationException if the device does not support
     *                                       mastership handling
     */
    void roleChanged(MastershipRole newRole);

    /**
     * Notifies the device of a mastership role change as decided by the core.
     * Differently from {@link #roleChanged(MastershipRole)}, the role is
     * described by the given preference value, where {@code preference = 0}
     * signifies {@link MastershipRole#MASTER} role and {@code preference > 0}
     * signifies {@link MastershipRole#STANDBY}. Smaller preference values
     * indicates higher mastership priority for different nodes.
     * <p>
     * This method does not permit notifying role {@link MastershipRole#NONE},
     * in which case {@link #roleChanged(MastershipRole)} should be used
     * instead.
     * <p>
     * Term is a monotonically increasing number, increased by one every time a
     * new master is elected.
     * <p>
     * The implementation of this method should trigger a {@link
     * DeviceAgentEvent} signaling the mastership role accepted by the device.
     *
     * @param preference preference value, where 0 signifies {@link
     *                   MastershipRole#MASTER} and all other values {@link
     *                   MastershipRole#STANDBY}
     * @param term       term number
     * @throws UnsupportedOperationException if the device does not support
     *                                       mastership handling, or if it does
     *                                       not support setting preference-based
     *                                       mastership, and {@link #roleChanged(MastershipRole)}
     *                                       should be used instead
     */
    default void roleChanged(int preference, long term) {
        if (preference == 0) {
            roleChanged(MastershipRole.MASTER);
        } else {
            roleChanged(MastershipRole.STANDBY);
        }
    }

    /**
     * Returns the last known mastership role agreed by the device for this
     * node.
     *
     * @return mastership role
     */
    MastershipRole getRole();

    /**
     * Adds a device agent listener for the given provider ID.
     *
     * @param providerId provider ID
     * @param listener   device agent listener
     */
    default void addDeviceAgentListener(
            ProviderId providerId, DeviceAgentListener listener) {
        throw new UnsupportedOperationException(
                "Device agent listener registration not supported");
    }

    /**
     * Removes a device agent listener previously registered for the given
     * provider ID.
     *
     * @param providerId provider ID
     */
    default void removeDeviceAgentListener(ProviderId providerId) {
        throw new UnsupportedOperationException(
                "Device agent listener removal not supported");
    }

}
