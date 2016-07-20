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
package org.onosproject.cpman.message;

import org.onosproject.cpman.ControlMessage;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of control message.
 */
public interface ControlMessageStore
        extends Store<ControlMessageEvent, ControlMessageStoreDelegate> {

    /**
     * Updates the control message statistics of the specified device.
     *
     * @param providerId  provider identifier
     * @param deviceId    device identifier
     * @param controlMessages a collection of control message stats
     * @return ready to send event describing what occurred
     */
    ControlMessageEvent updateStatsInfo(ProviderId providerId, DeviceId deviceId,
                                        Set<ControlMessage> controlMessages);

}
