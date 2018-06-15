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
package org.onosproject.netconf;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onlab.util.Tools;

/**
 * Adapter mainly intended for usage in tests.
 */
public class NetconfSessionAdapter
    extends AbstractNetconfSession
    implements NetconfSession {

    // TODO remove methods defined in AbstractNetconfSession

    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
    }

    @Override
    public void startSubscription() throws NetconfException {
    }

    @Override
    public String requestSync(String request) throws NetconfException {
        return null;
    }

    @Override
    public CompletableFuture<String> request(String request)
            throws NetconfException {
        return Tools.exceptionalFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<String> rpc(String request)
            throws NetconfException {
        return Tools.exceptionalFuture(new UnsupportedOperationException());
    }


    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public String get(String filterSchema, String withDefaultsMode)
            throws NetconfException {
        return null;
    }

    @Override
    public String get(String request) throws NetconfException {
        return null;
    }

    @Override
    public void endSubscription() throws NetconfException {
    }

    @Override
    public boolean editConfig(String newConfiguration) throws NetconfException {
        return true;
    }

    @Override
    public String doWrappedRpc(String request) throws NetconfException {
        return null;
    }

    @Override
    public boolean copyConfig(DatastoreId destination, DatastoreId source)
            throws NetconfException {
        return true;
    }

    @Override
    public boolean copyConfig(DatastoreId netconfTargetConfig,
                              String newConfiguration)
            throws NetconfException {
        return true;
    }

    @Override
    public boolean copyConfig(String netconfTargetConfig,
                              String newConfiguration)
            throws NetconfException {
        return true;
    }

    @Override
    public boolean close() throws NetconfException {
        return true;
    }

    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
    }

    @Override
    public String getConfig(DatastoreId netconfTargetConfig)
            throws NetconfException {
        return null;
    }

    @Override
    public String getConfig(DatastoreId netconfTargetConfig,
                            String configurationFilterSchema)
            throws NetconfException {
        return null;
    }

    @Override
    public boolean editConfig(DatastoreId netconfTargetConfig, String mode,
                              String newConfiguration)
            throws NetconfException {
        return true;
    }

    @Override
    public boolean deleteConfig(DatastoreId netconfTargetConfig)
            throws NetconfException {
        return true;
    }

    @Override
    public boolean lock(DatastoreId datastore) throws NetconfException {
        return true;
    }

    @Override
    public boolean unlock(DatastoreId datastore) throws NetconfException {
        return true;
    }

    @Override
    public Set<String> getDeviceCapabilitiesSet() {
        return Collections.emptySet();
    }
}