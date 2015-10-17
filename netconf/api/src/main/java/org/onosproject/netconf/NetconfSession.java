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

package org.onosproject.netconf;

import java.util.List;

/**
 * NETCONF session object that allows NETCONF operations on top with the physical
 * device on top of an SSH connection.
 */
// TODO change return type of methdos to <Capability, XMLdoc, string or yang obj>
public interface NetconfSession {

    /**
     * Retrives the requested configuration, different from get-config.
     * @param request the XML containing the request to the server.
     * @return device running configuration
     */
    String get(String request);

    /**
     * Executes an RPC to the server.
     * @param request the XML containing the RPC for the server.
     * @return Server response or ERROR
     */
    String doRPC(String request);

    /**
     * Retrives the specified configuration.
     *
     * @param targetConfiguration the type of configuration to retrieve.
     * @return specified configuration.
     */
    String getConfig(String targetConfiguration);

    /**
     * Retrives part of the specivied configuration based on the filterSchema.
     *
     * @param targetConfiguration       the type of configuration to retrieve.
     * @param configurationFilterSchema XML schema to filter the configuration
     *                                  elements we are interested in
     * @return device running configuration.
     */
    String getConfig(String targetConfiguration, String configurationFilterSchema);

    /**
     * Retrives part of the specified configuration based on the filterSchema.
     *
     * @param newConfiguration configuration to set
     * @return true if the configuration was edited correctly
     */

    boolean editConfig(String newConfiguration);

    /**
     * Copies the new configuration, an Url or a complete configuration xml tree
     * to the target configuration.
     * The target configuration can't be the running one
     *
     * @param targetConfiguration the type of configuration to retrieve.
     * @param newConfiguration    configuration to set
     * @return true if the configuration was copied correctly
     */
    boolean copyConfig(String targetConfiguration, String newConfiguration);

    /**
     * Deletes part of the specified configuration based on the filterSchema.
     *
     * @param targetConfiguration the name of the configuration to delete
     * @return true if the configuration was copied correctly
     */
    boolean deleteConfig(String targetConfiguration);

    /**
     * Locks the candidate configuration.
     *
     * @return true if successful.
     */
    boolean lock();

    /**
     * Unlocks the candidate configuration.
     *
     * @return true if successful.
     */
    boolean unlock();

    /**
     * Closes the Netconf session with the device.
     * the first time it tries gracefully, then kills it forcefully
     * @return true if closed
     */
    boolean close();

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
     * Sets the device capabilities.
     * @param capabilities list of capabilities the device has.
     */
    void setDeviceCapabilities(List<String> capabilities);

}
