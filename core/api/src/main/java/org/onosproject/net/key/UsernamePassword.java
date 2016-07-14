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

package org.onosproject.net.key;

/**
 * Representation of a username and password.
 */
public final class UsernamePassword extends DeviceKey {
    private final String username;
    private final String password;

    /**
     * Private constructor for a username and password.
     *
     * @param username user's name
     * @param password user's password
     */
    private UsernamePassword(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Static method to construct a username / password.
     *
     * @param username user's name
     * @param password user's password
     * @return username and password
     */
    static UsernamePassword usernamePassword(String username, String password) {
        return new UsernamePassword(username, password);
    }

    /**
     * Returns the username.
     *
     * @return username
     */
    public String username() {
        return username;
    }

    /**
     * Returns the password.
     *
     * @return password
     */
    public String password() {
        return password;
    }
}
