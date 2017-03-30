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
package org.onosproject.vpls.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing the VPLS command type.
 */
public enum VplsCommandEnum {
    ADD_IFACE("add-if"),
    CREATE("create"),
    DELETE("delete"),
    LIST("list"),
    REMOVE_IFACE("rem-if"),
    SET_ENCAP("set-encap"),
    SHOW("show"),
    CLEAN("clean");

    private final String command;

    /**
     * Creates the enum from a string representing the command.
     *
     * @param command the text representing the command
     */
    VplsCommandEnum(final String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command;
    }

    /**
     * Returns a list of command string values.
     *
     * @return the list of string values corresponding to the enums
     */
    public static List<String> toStringList() {
        return Arrays.stream(values())
                .map(VplsCommandEnum::toString)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Alternative method to valueOf. It returns the command type
     * corresponding to the given string. If the parameter does not match a
     * constant name, or is null, null is returned.
     *
     * @param command the string representing the encapsulation type
     * @return the EncapsulationType constant corresponding to the string given
     */
    public static VplsCommandEnum enumFromString(String command) {
        if (command != null && !command.isEmpty()) {
            for (VplsCommandEnum c : values()) {
                if (command.equalsIgnoreCase(c.toString())) {
                    return c;
                }
            }
        }
        return null;
    }
}
