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
package org.onosproject.pcelabelstore.api;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.incubator.net.resource.label.LabelResourceId;

/**
 * Abstraction of an entity providing LSP local label information.
 */
public interface LspLocalLabelInfo {

    /**
     * Returns device id.
     *
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns in label id of a device.
     *
     * @return in label resource id
     */
    LabelResourceId inLabelId();

    /**
     * Returns out label id of a device.
     *
     * @return node out label resource id
     */
    LabelResourceId outLabelId();

    /**
     * Returns in port of an incoming label.
     *
     * @return in port
     */
    PortNumber inPort();

    /**
     * Returns next hop of an outgoing label.
     *
     * @return out port
     */
    PortNumber outPort();

    /**
     * LspLocalLabelInfo Builder.
     */
    interface Builder {

        /**
         * Returns builder object of a device id.
         *
         * @param id device id
         * @return builder object of device id
         */
        Builder deviceId(DeviceId id);

        /**
         * Returns builder object of in label.
         *
         * @param id in label id
         * @return builder object of in label id
         */
        Builder inLabelId(LabelResourceId id);

        /**
         * Returns builder object of out label.
         *
         * @param id out label id
         * @return builder object of out label id
         */
        Builder outLabelId(LabelResourceId id);

        /**
         * Returns builder object of in port of an incoming label.
         *
         * @param port in port
         * @return builder object of in port
         */
        Builder inPort(PortNumber port);

        /**
         * Returns builder object of next hop of an outgoing label.
         *
         * @param port out port
         * @return builder object of out port
         */
        Builder outPort(PortNumber port);

        /**
         * Builds object of device local label info.
         *
         * @return object of device local label info.
         */
        LspLocalLabelInfo build();
    }
}
