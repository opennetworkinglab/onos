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

package org.onosproject.bmv2.api.service;

import com.google.common.annotations.Beta;
import org.onosproject.bmv2.api.runtime.Bmv2Device;

/**
 * A listener of BMv2 device events.
 */
@Beta
public interface Bmv2DeviceListener {

    /**
     * Handles a hello message.
     *
     * @param device        the BMv2 device that originated the message
     * @param instanceId    the ID of the BMv2 process instance
     * @param jsonConfigMd5 the MD5 sum of the JSON configuration currently running on the device
     */
    void handleHello(Bmv2Device device, int instanceId, String jsonConfigMd5);
}
