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
package org.onosproject.net.behaviour;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ControlProtocolVersion {
    OF_1_0("OpenFlow10"),
    OF_1_1("OpenFlow11"),
    OF_1_2("OpenFlow12"),
    OF_1_3("OpenFlow13"),
    OF_1_4("OpenFlow14"),
    OF_1_5("OpenFlow15");

    private final String versionString;

    /**
     * Creates the enum from a string representing the control protoocol version.
     *
     * @param versionString the text representing the control protocol version.
     */
    ControlProtocolVersion(final String versionString) {
        this.versionString = versionString;
    }

    @Override
    public String toString() {
        return versionString;
    }

    /**
     * Returns a list of control protocol version string values.
     *
     * @return the list of string values corresponding to the enums
     */
    public static List<String> toStringList() {
        return Arrays.stream(values())
                .map(ControlProtocolVersion::toString)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Alternative method to valueOf. It returns the ControlProtocolVersion type
     * corresponding to the given string. If the parameter does not match a
     * constant name, or is null, null is returned.
     *
     * @param versionString the string representing the encapsulation type
     * @return the EncapsulationType constant corresponding to the string given
     */
    public static ControlProtocolVersion enumFromString(String versionString) {
        if (versionString != null && !versionString.isEmpty()) {
            for (ControlProtocolVersion c : values()) {
                if (versionString.equalsIgnoreCase(c.toString())) {
                    return c;
                }
            }
        }
        return null;
    }

}
