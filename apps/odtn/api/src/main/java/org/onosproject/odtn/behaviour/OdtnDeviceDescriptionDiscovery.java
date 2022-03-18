/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.odtn.behaviour;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;

import com.google.common.annotations.Beta;

/**
 * DeviceDescriptionDiscovery used in ODTN.
 *
 * Just declaring certain Annotations will be required.
 */
@Beta
public interface OdtnDeviceDescriptionDiscovery
        extends DeviceDescriptionDiscovery {

    /**
     * Annotations key intended for an OpenConfig generic component, which stores component name.
     * Typically used for Ports
     */
    String OC_NAME = "oc-name";

    /**
     * Annotations key intended for an OpenConfig generic component, which stores component type.
    */
    String OC_TYPE = "oc-type";

    /**
     * Annotations key intended for a Port, which stores OpenConfig optical channel component associated to the port.
     */
    String OC_OPTICAL_CHANNEL_NAME = "oc-optical-channel-name";

    /**
     * Annotations key intended for a Port, which stores OpenConfig transceiver associated to the port.
     */
    String OC_TRANSCEIVER_NAME = "oc-transceiver-name";


    /**
     * Annotations key intended for a Port, which stores OpenConfig logical channel associated to the port.
     */
    String OC_LOGICAL_CHANNEL = "oc-logical-channel";

    /**
     * Annotations key intended for a Port,
     * which stores string identifier used to
     * logically group Ports corresponding to a transponder, etc.
     */
    String CONNECTION_ID = "odtn-connection-id";

    /**
     * Annotations key for a Port,
     * which describes role of the port annotated.
     * Value must be one of “client” or “line”.
     *
     * @see OdtnPortType
     */
    String PORT_TYPE = "odtn-port-type";

    enum OdtnPortType {
        CLIENT("client"),
        LINE("line");

        private final String value;

        OdtnPortType(String value) {
            this.value = value;
        }

        /**
         * Returns the value to be used as Annotations value.
         * @return value
         */
        public String value() {
            return value;
        }

        /**
         * Returns the corresponding enum value from a string value.
         * @param value to look up
         * @return OdtnPortType
         *
         * @throws NullPointerException if {@code value} was null
         * @throws IllegalArgumentException if non-OdtnPortValue was given
         */
        public static OdtnPortType fromValue(String value) {
            checkNotNull(value);
            if (value.equalsIgnoreCase(CLIENT.value())) {
                return CLIENT;
            } else if (value.equalsIgnoreCase(LINE.value())) {
                return LINE;
            } else {
                throw new IllegalArgumentException("Invalid value: " + value);
            }
        }
    }

    /**
     * OpenConfig component property name to store,
     * decimal integer index to be used when creating PortNumber.
     * <p>
     * Optional if providing original implementation other than
     * odtn-driver supplied driver.
     */
    String ONOS_PORT_INDEX = "onos-index";

    // overriding just to make checkstyle happy
    @Override
    List<PortDescription> discoverPortDetails();

}
