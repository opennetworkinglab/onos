/*
 * Copyright 2015-present Open Networking Laboratory
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * NETCONF session object that allows NETCONF operations on top with the physical
 * device on top of an SSH connection.
 */
// TODO change return type of methdos to <Capability, XMLdoc, string or yang obj>
public interface NetconfSession {

    /**
     * Executes an asynchronous RPC to the server and obtains a future to be completed.
     *
     * @param request the XML containing the RPC for the server.
     * @return Server response or ERROR
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    CompletableFuture<String> request(String request) throws NetconfException;


    /**
     * Retrives the requested configuration, different from get-config.
     *
     * @param request the XML containing the request to the server.
     * @return device running configuration
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String get(String request) throws NetconfException;

    /**
     * Retrives the requested data.
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
     * Retrives the specified configuration.
     *
     * @param targetConfiguration the type of configuration to retrieve.
     * @return specified configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String getConfig(String targetConfiguration) throws NetconfException;

    /**
     * Retrives part of the specivied configuration based on the filterSchema.
     *
     * @param targetConfiguration       the type of configuration to retrieve.
     * @param configurationFilterSchema XML schema to filter the configuration
     *                                  elements we are interested in
     * @return device running configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    String getConfig(String targetConfiguration, String configurationFilterSchema)
            throws NetconfException;

    /**
     * Retrives part of the specified configuration based on the filterSchema.
     *
     * @param newConfiguration configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */

    boolean editConfig(String newConfiguration) throws NetconfException;

    /**
     * Retrives part of the specified configuration based on the filterSchema.
     *
     * @param targetConfiguration the targetConfiguration to change
     * @param mode                selected mode to change the configuration
     * @param newConfiguration    configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean editConfig(String targetConfiguration, String mode, String newConfiguration)
            throws NetconfException;

    /**
     * Copies the new configuration, an Url or a complete configuration xml tree
     * to the target configuration.
     * The target configuration can't be the running one
     *
     * @param targetConfiguration the type of configuration to retrieve.
     * @param newConfiguration    configuration to set
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean copyConfig(String targetConfiguration, String newConfiguration)
            throws NetconfException;

    /**
     * Deletes part of the specified configuration based on the filterSchema.
     *
     * @param targetConfiguration the name of the configuration to delete
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean deleteConfig(String targetConfiguration) throws NetconfException;

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
     * @param configType type of configuration to be locked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean lock(String configType) throws NetconfException;

    /**
     * Unlocks the specified configuration.
     *
     * @param configType type of configuration to be locked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean unlock(String configType) throws NetconfException;

    /**
     * Locks the running configuration.
     *
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean lock() throws NetconfException;

    /**
     * Unlocks the running configuration.
     *
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    boolean unlock() throws NetconfException;

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
     * Gets the capabilities of the Netconf server associated to this session.
     *
     * @return Network capabilities as a string.
     */
    String getServerCapabilities();

    /**
     * Sets the ONOS side capabilities.
     *
     * @param capabilities list of capabilities the device has.
     */
    void setDeviceCapabilities(List<String> capabilities);

    /**
     * Remove a listener from the underlying stream handler implementation.
     *
     * @param listener event listener.
     */
    void addDeviceOutputListener(NetconfDeviceOutputEventListener listener);

    /**
     * Remove a listener from the underlying stream handler implementation.
     *
     * @param listener event listener.
     */
    void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener);

}
