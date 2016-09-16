/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Mock NetconfSessionImpl.
 */
public class FujitsuNetconfSessionMock implements NetconfSession {

    private FujitsuNetconfSessionListenerTest listener;

    /**
     * Registers a listener to be invoked for verification.
     *
     * @param listener listener to be added
     */
    public void setListener(FujitsuNetconfSessionListenerTest listener) {
        this.listener = listener;
    }

    @Override
    public CompletableFuture<String> request(String request) throws NetconfException {
        return null;
    }

    @Override
    public String get(String request) throws NetconfException {
        return null;
    }

    @Override
    public String get(String filterSchema, String withDefaultsMode)
            throws NetconfException {
        boolean result = true;
        String reply = null;
        if (listener != null) {
            result = listener.verifyGet(filterSchema, withDefaultsMode);
            if (result) {
                reply = listener.buildGetReply();
            }
        }
        return result ?  ((reply == null) ? filterSchema : reply) : null;
    }

    @Override
    public String doWrappedRpc(String request) throws NetconfException {
        boolean result = true;
        if (listener != null) {
            result = listener.verifyWrappedRpc(request);
        }
        return result ? request : null;
    }

    @Override
    public String requestSync(String request) throws NetconfException {
        return null;
    }

    @Override
    public String getConfig(String targetConfiguration) throws NetconfException {
        return null;
    }

    @Override
    public String getConfig(String targetConfiguration, String configurationFilterSchema)
            throws NetconfException {
        return null;
    }

    @Override
    public boolean editConfig(String newConfiguration) throws NetconfException {
        boolean result = true;
        if (listener != null) {
            result = listener.verifyEditConfig(newConfiguration);
        }
        return result;
    }

    @Override
    public boolean editConfig(String targetConfiguration, String mode, String newConfiguration)
            throws NetconfException {
        boolean result = true;
        if (listener != null) {
            result = listener.verifyEditConfig(targetConfiguration, mode, newConfiguration);
        }
        return result;
    }

    @Override
    public boolean copyConfig(String targetConfiguration, String newConfiguration)
            throws NetconfException {
        return false;
    }

    @Override
    public boolean deleteConfig(String targetConfiguration) throws NetconfException {
        return false;
    }

    @Override
    public void startSubscription() throws NetconfException {
    }

    @Beta
    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        if (listener != null) {
            listener.verifyStartSubscription(filterSchema);
        }
        return;
    }

    @Override
    public void endSubscription() throws NetconfException {
    }

    @Override
    public boolean lock(String configType) throws NetconfException {
        return false;
    }

    @Override
    public boolean unlock(String configType) throws NetconfException {
        return false;
    }

    @Override
    public boolean lock() throws NetconfException {
        return false;
    }

    @Override
    public boolean unlock() throws NetconfException {
        return false;
    }

    @Override
    public boolean close() throws NetconfException {
        return false;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public String getServerCapabilities() {
        return null;
    }

    @Override
    public void setDeviceCapabilities(List<String> capabilities) {
    }

    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
    }

    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
    }

}
