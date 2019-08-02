/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.netconf;

import com.google.common.annotations.Beta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * NETCONF session object that allows NETCONF operations on top with the physical
 * device on top of an SSH connection.
 */
// TODO change return type of methods to <Capability, XMLdoc, string or yang obj>
public interface NetconfSession {

    /**
     * Executes an asynchronous RPC to the server and obtains a future to be completed.
     *
     * The caller must ensure that the message-id in any request is unique
     * for the session
     *
     * @deprecated  - 1.10.0 do not remove needs reworking
     * @param request the XML containing the RPC for the server.
     * @return Server response or ERROR
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    @Deprecated
    CompletableFuture<String> request(String request) throws NetconfException;

    /**
     * Executes an asynchronous RPC request to the server and obtains a future
     * for it's response.
     *
     * @param request the XML containing the RPC request for the server.
     * @return Server response or ERROR
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @throws NetconfTransportException on secure transport-layer error
     */
    CompletableFuture<String> rpc(String request) throws NetconfException;

    /**
     * Retrieves the specified configuration.
     *
     * @param datastore to retrieve configuration from
     * @return specified configuration
     *
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    CompletableFuture<CharSequence> asyncGetConfig(DatastoreId datastore) throws NetconfException;

    /**
     * Retrieves running configuration and device state.
     *
     * @return running configuration and device state
     *
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    CompletableFuture<CharSequence> asyncGet() throws NetconfException;


    /**
     * Retrieves the requested configuration, different from get-config.
     *
     * @param request the XML containing the request to the server.
     * @return device running configuration
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String get(String request) throws NetconfException;

    /**
     * Retrieves the requested data.
     *
     * @param filterSchema XML subtrees to include in the reply
     * @param withDefaultsMode with-defaults mode
     * @return Server response
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String get(String filterSchema, String withDefaultsMode)
            throws NetconfException;

    /**
     * Executes an synchronous RPC to the server and wrap the request in RPC header.
     *
     * @param request the XML containing the request to the server.
     * @return Server response or ERROR
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String doWrappedRpc(String request) throws NetconfException;

    /**
     * Executes an synchronous RPC to the server.
     *
     * @param request the XML containing the RPC for the server.
     * @return Server response or ERROR
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String requestSync(String request) throws NetconfException;

    /**
     * Executes an synchronous RPC to the server with specific reply TIMEOUT.
     *
     * @param request the XML containing the RPC for the server.
     * @param timeout the reply timeout.
     * @return Server response or ERROR
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default String requestSync(String request, int timeout) throws NetconfException {
        return "";
    }

    /**
     * Retrieves the specified configuration.
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @return specified configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     *
     * @deprecated in 1.13.0 use async version instead.
     */
    @Deprecated
    String getConfig(DatastoreId netconfTargetConfig) throws NetconfException;

    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig       the type of configuration to retrieve.
     * @param configurationFilterSchema XML schema to filter the configuration
     *                                  elements we are interested in
     * @return device running configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String getConfig(DatastoreId netconfTargetConfig,
                             String configurationFilterSchema)
            throws NetconfException;

    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param newConfiguration configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean editConfig(String newConfiguration) throws NetconfException;

    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the targetConfiguration to change
     * @param mode                default-operation mode
     * @param newConfiguration    configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean editConfig(DatastoreId netconfTargetConfig, String mode, String newConfiguration)
            throws NetconfException;

    /**
     * Copies the configuration between configuration datastores.
     * <p>
     * The target configuration can't be the running one
     *
     * @param destination configuration datastore
     * @param source configuration datastore
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean copyConfig(DatastoreId destination, DatastoreId source)
            throws NetconfException;

    /**
     * Copies the new configuration, an Url or a complete configuration xml tree
     * to the target configuration.
     * The target configuration can't be the running one
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @param newConfiguration configuration XML to set or URL tag to the configuration
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
     boolean copyConfig(DatastoreId netconfTargetConfig, String newConfiguration)
            throws NetconfException;

    /**
     * Copies the new configuration, an Url or a complete configuration xml tree
     * to the target configuration.
     * The target configuration can't be the running one
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @param newConfiguration    configuration to set
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean copyConfig(String netconfTargetConfig, String newConfiguration)
            throws NetconfException;

    /**
     * Deletes part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the name of the configuration to delete
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean deleteConfig(DatastoreId netconfTargetConfig) throws NetconfException;

    /**
     * Starts subscription to the device's notifications.
     *
     * @throws NetconfException when there is a problem starting the subscription
     */
    void startSubscription() throws NetconfException;

    /**
     * Starts subscription to the device's notifications.
     *
     * @param filterSchema XML subtrees to indicate specific notification
     * @throws NetconfException when there is a problem starting the subscription
     */
    @Beta
    void startSubscription(String filterSchema) throws NetconfException;

    /**
     * Ends subscription to the device's notifications.
     *
     * @throws NetconfException when there is a problem ending the subscription
     */
    void endSubscription() throws NetconfException;

    /**
     * Locks the specified configuration.
     *
     * @param datastore configuration datastore to be locked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean lock(DatastoreId datastore) throws NetconfException;

    /**
     * Unlocks the specified configuration.
     *
     * @param datastore configuration datastore to be unlocked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean unlock(DatastoreId datastore) throws NetconfException;

    /**
     * Locks the running configuration.
     *
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default boolean lock() throws NetconfException {
        return lock(DatastoreId.RUNNING);
    }

    /**
     * Unlocks the running configuration.
     *
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default boolean unlock() throws NetconfException {
        return unlock(DatastoreId.RUNNING);
    }

    /**
     * Commits the candidate configuration the running configuration.
     *
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default boolean commit() throws NetconfException {
        return false;
    }

    /**
     * Closes the Netconf session with the device.
     * the first time it tries gracefully, then kills it forcefully
     *
     * @return true if closed
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean close() throws NetconfException;

    /**
     * Gets the session ID of the Netconf session.
     *
     * @return Session ID as a string.
     */
    String getSessionId();

    /**
     * Gets the capabilities of the remote Netconf device associated to this
     * session.
     *
     * @return Network capabilities as strings in a Set.
     * @since 1.10.0
     */
    Set<String> getDeviceCapabilitiesSet();

    /**
     * Checks the state of the underlying SSH session and connection
     * and if necessary it reestablishes it.
     * Should be implemented, providing a default here for retrocompatibility.
     * @throws NetconfException when there is a problem in reestablishing
     * the connection or the session to the device.
     */
    default void checkAndReestablish() throws NetconfException {
        Logger log = LoggerFactory.getLogger(NetconfSession.class);
        log.error("Not implemented/exposed by the underlying session implementation");
    }

    /**
     * Sets the ONOS side capabilities.
     *
     * @param capabilities list of capabilities ONOS has.
     * @since 1.10.0
     */
    default void setOnosCapabilities(Iterable<String> capabilities) {
        // default implementation should be removed in the future
        // no-op
    }

    /**
     * Add a listener to the underlying stream handler implementation.
     *
     * @param listener event listener.
     * @throws NetconfException when this method will be called by STANDBY or NONE node.
     */
    void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) throws NetconfException;

    /**
     * Remove a listener from the underlying stream handler implementation.
     *
     * @param listener event listener.
     * @throws NetconfException when this method will be called by STANDBY or NONE node.
     */
    void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) throws NetconfException;

    /**
     * Read the connect timeout that this session was created with.
     * @return timeout in seconds
     */
    default int timeoutConnectSec() {
        return 0;
    }

    /**
     * Read the reply timeout that this session was created with.
     * @return timeout in seconds
     */
    default int timeoutReplySec() {
        return 0;
    }

    /**
     * Read the idle timeout that this session was created with.
     * @return timeout in seconds
     */
    default int timeoutIdleSec() {
        return 0;
    }

}
