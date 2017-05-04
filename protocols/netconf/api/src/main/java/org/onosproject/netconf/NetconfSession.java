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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Retrieves the specified configuration.
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @return specified configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default String getConfig(DatastoreId netconfTargetConfig) throws NetconfException {
        // default implementation provided for backward compatibility
        // this API is the one, which should be implemented
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return getConfig(netconfTargetConfig.id());
    }

    /**
     * Retrieves the specified configuration.
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @return specified configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration parameter instead
     */
    @Deprecated
    default String getConfig(TargetConfig netconfTargetConfig) throws NetconfException {
        return getConfig(TargetConfig.toDatastoreId(netconfTargetConfig));
    }

    /**
     * Retrieves the specified configuration.
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @return specified configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration parameter instead
     */
    @Deprecated
    default String getConfig(String netconfTargetConfig) throws NetconfException {
        return getConfig(TargetConfig.toDatastoreId(netconfTargetConfig));
    }


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
    default String getConfig(DatastoreId netconfTargetConfig,
                             String configurationFilterSchema)
            throws NetconfException {
        // default implementation provided for backward compatibility
        // this API is the one, which should be implemented
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return getConfig(netconfTargetConfig.id(), configurationFilterSchema);
    }


    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig       the type of configuration to retrieve.
     * @param configurationFilterSchema XML schema to filter the configuration
     *                                  elements we are interested in
     * @return device running configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfig enum parameter instead
     */
    @Deprecated
    default String getConfig(String netconfTargetConfig, String configurationFilterSchema)
            throws NetconfException {
        return getConfig(TargetConfig.toDatastoreId(netconfTargetConfig),
                         configurationFilterSchema);
    }

    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig       the type of configuration to retrieve.
     * @param configurationFilterSchema XML schema to filter the configuration
     *                                  elements we are interested in
     * @return device running configuration.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfig enum parameter instead
     */
    @Deprecated
    default String getConfig(TargetConfig netconfTargetConfig, String configurationFilterSchema)
            throws NetconfException {
        return getConfig(TargetConfig.toDatastoreId(netconfTargetConfig),
                         configurationFilterSchema);
    }


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
     * @param mode                selected mode to change the configuration
     * @param newConfiguration    configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default boolean editConfig(DatastoreId netconfTargetConfig, String mode, String newConfiguration)
            throws NetconfException {
        // default implementation provided for backward compatibility
        // this API is the one, which should be implemented
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return editConfig(netconfTargetConfig.id(), mode, newConfiguration);
    }

    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the targetConfiguration to change
     * @param mode                selected mode to change the configuration
     * @param newConfiguration    configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration enum parameter instead
     */
    @Deprecated
    default boolean editConfig(String netconfTargetConfig, String mode, String newConfiguration)
            throws NetconfException {
        return editConfig(TargetConfig.toDatastoreId(netconfTargetConfig),
                          mode,
                          newConfiguration);
    }

    /**
     * Retrieves part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the targetConfiguration to change
     * @param mode                selected mode to change the configuration
     * @param newConfiguration    configuration to set
     * @return true if the configuration was edited correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration enum parameter instead
     */
    @Deprecated
    default boolean editConfig(TargetConfig netconfTargetConfig, String mode, String newConfiguration)
            throws NetconfException {
        return editConfig(TargetConfig.toDatastoreId(netconfTargetConfig),
                          mode,
                          newConfiguration);
    }

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
    default boolean copyConfig(DatastoreId destination, DatastoreId source)
            throws NetconfException {
        // default implementation provided for backward compatibility
        // but this API should be implemented overriding the default
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return copyConfig(destination.id(), source.id());
    }

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
    default boolean copyConfig(DatastoreId netconfTargetConfig, String newConfiguration)
            throws NetconfException {
        // default implementation provided for backward compatibility
        // but this API should be implemented overriding the default
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return copyConfig(netconfTargetConfig.id(), newConfiguration);
    }

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
     * Copies the new configuration, an Url or a complete configuration xml tree
     * to the target configuration.
     * The target configuration can't be the running one
     *
     * @param netconfTargetConfig the type of configuration to retrieve.
     * @param newConfiguration    configuration to set
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration enum parameter instead
     */
    @Deprecated
    default boolean copyConfig(TargetConfig netconfTargetConfig, String newConfiguration)
            throws NetconfException {
        return copyConfig(TargetConfig.toDatastoreId(netconfTargetConfig), newConfiguration);
    }

    /**
     * Deletes part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the name of the configuration to delete
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default boolean deleteConfig(DatastoreId netconfTargetConfig) throws NetconfException {
        // default implementation provided for backward compatibility
        // this API is the one, which should be implemented
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return deleteConfig(netconfTargetConfig.id());
    }

    /**
     * Deletes part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the name of the configuration to delete
     * @return true if the configuration was deleted correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration enum parameter instead
     */
    @Deprecated
    default boolean deleteConfig(String netconfTargetConfig) throws NetconfException {
        return deleteConfig(TargetConfig.toDatastoreId(netconfTargetConfig));
    }

    /**
     * Deletes part of the specified configuration based on the filterSchema.
     *
     * @param netconfTargetConfig the name of the configuration to delete
     * @return true if the configuration was copied correctly
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration enum parameter instead
     */
    @Deprecated
    default boolean deleteConfig(TargetConfig netconfTargetConfig) throws NetconfException {
        return deleteConfig(TargetConfig.toDatastoreId(netconfTargetConfig));
    }

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
    default boolean lock(DatastoreId datastore) throws NetconfException {
        // default implementation provided for backward compatibility
        // this API is the one, which should be implemented
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return lock(datastore.id());
    }

    /**
     * Locks the specified configuration.
     *
     * @param configType type of configuration to be locked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration parameter instead
     */
    @Deprecated
    default boolean lock(String configType) throws NetconfException {
        return lock(TargetConfig.toDatastoreId(configType));
    }

    /**
     * Unlocks the specified configuration.
     *
     * @param datastore configuration datastore to be unlocked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     */
    default boolean unlock(DatastoreId datastore) throws NetconfException {
        // default implementation provided for backward compatibility
        // this API is the one, which should be implemented
        // TODO default implementation here should be removed after
        // deprecation of the other 2 variants.
        return unlock(datastore.id());
    }

    /**
     * Unlocks the specified configuration.
     *
     * @param configType type of configuration to be locked
     * @return true if successful.
     * @throws NetconfException when there is a problem in the communication process on
     * the underlying connection
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfiguration parameter instead
     */
    @Deprecated
    default boolean unlock(String configType) throws NetconfException {
        return unlock(TargetConfig.toDatastoreId(configType));
    }

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
     *
     * @since 1.10.0
     * Note: default implementation provided with the interface
     * will be removed when {@code getServerCapabilities()} reaches
     * deprecation grace period.
     */
    default Set<String> getDeviceCapabilitiesSet() {
        // default implementation should be removed in the future
        Set<String> capabilities = new LinkedHashSet<>();
        Matcher capabilityMatcher =
                Pattern.compile("<capability>\\s*(.*?)\\s*</capability>")
                       .matcher(getServerCapabilities());
        while (capabilityMatcher.find()) {
            capabilities.add(capabilityMatcher.group(1));
        }
        return capabilities;
    }

    /**
     * Gets the capabilities of the Netconf server (remote device) associated
     * to this session.
     *
     * @return Network capabilities as a string.
     * @deprecated 1.10.0 use {@link #getDeviceCapabilitiesSet()} instead
     */
    @Deprecated
    String getServerCapabilities();

    /**
     * Sets the ONOS side capabilities.
     *
     * @param capabilities list of capabilities the device has.
     * @deprecated 1.10.0 use {@link #setOnosCapabilities(Iterable)} instead
     */
    @Deprecated
    void setDeviceCapabilities(List<String> capabilities);

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
     *
     * @since 1.10.0
     */
    default void setOnosCapabilities(Iterable<String> capabilities) {
        // default implementation should be removed in the future
        // no-op
    }

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
