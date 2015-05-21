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
 *
 */

package org.onosproject.cord.gui.model;

/**
 * Designates a user of a subscriber's account.
 */
public class SubscriberUser {
    private final int id;
    private final String name;
    private final String mac;

    /**
     * Constructs a subscriber user from the given parameters.
     *
     * @param id internal identifier
     * @param name display name
     * @param mac MAC address of the associated device
     */
    public SubscriberUser(int id, String name, String mac) {
        this.id = id;
        this.name = name;
        this.mac = mac;
    }

    /**
     * Returns the internal identifier.
     *
     * @return the identifier
     */
    public int id() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return display name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the MAC address of the associated device.
     *
     * @return MAC address
     */
    public String mac() {
        return mac;
    }
}
