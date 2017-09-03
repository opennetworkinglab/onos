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

package org.onosproject.drivers.fujitsu;


import org.onosproject.netconf.DatastoreId;

public interface FujitsuNetconfSessionListenerTest {

    /**
     * Verify editConfig request arguments.
     *
     * @param newConfiguration    configuration to set
     * @return true if everuthing as expected
     */
    boolean verifyEditConfig(String newConfiguration);

    /**
     * Verify editConfig request arguments.
     *
     * @param targetConfiguration the targetConfiguration to change
     * @param mode                selected mode to change the configuration
     * @param newConfiguration    configuration to set
     * @return true if everuthing as expected
     * @deprecated - 1.10.0 Kingfisher use method overload that accepts
     * org.onosproject.netconf.TargetConfig enum parameter instead
     */
    @Deprecated
    boolean verifyEditConfig(String targetConfiguration, String mode, String newConfiguration);

    /**
     * Verify editConfig request arguments.
     *
     * @param targetConfiguration the targetConfiguration to change
     * @param mode                selected mode to change the configuration
     * @param newConfiguration    configuration to set
     * @return true if everuthing as expected
     */
    boolean verifyEditConfig(DatastoreId targetConfiguration, String mode, String newConfiguration);

    /**
     * Verify get request arguments.
     *
     * @param filterSchema XML subtrees to include in the reply
     * @param withDefaultsMode with-defaults mode
     * @return true if everuthing as expected
     */
    boolean verifyGet(String filterSchema, String withDefaultsMode);

    /**
     * Build get RPC response if necessary.
     *
     * @return String or null if not support
     */
    String buildGetReply();

    /**
     * Verify rpc request arguments.
     *
     * @param request the XML containing the request to the server.
     * @return true if everuthing as expected
     */
    boolean verifyWrappedRpc(String request);

    /**
     * Verify rpc request arguments.
     *
     * @param filterSchema XML subtrees to indicate specific notification
     */
    void verifyStartSubscription(String filterSchema);

}
