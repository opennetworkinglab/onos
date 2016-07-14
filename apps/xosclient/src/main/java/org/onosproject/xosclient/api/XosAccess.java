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
package org.onosproject.xosclient.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Objects holding XOS API access information.
 */
public class XosAccess {

    private final String endpoint;
    private final String username;
    private final String password;

    /**
     * Default constructor.
     *
     * @param endpoint XOS service endpoint
     * @param adminUser admin user name
     * @param adminPassword admin password
     */
    public XosAccess(String endpoint, String adminUser, String adminPassword) {
        this.endpoint = checkNotNull(endpoint);
        this.username = checkNotNull(adminUser);
        this.password = checkNotNull(adminPassword);
    }

    /**
     * Returns XOS service endpoint.
     *
     * @return endpoint
     */
    public String endpoint() {
        return this.endpoint;
    }

    /**
     * Returns admin user name.
     *
     * @return user name
     */
    public String username() {
        return this.username;
    }

    /**
     * Returns admin password.
     *
     * @return password
     */
    public String password() {
        return this.password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, username, password);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj instanceof XosAccess)) {
            XosAccess that = (XosAccess) obj;
            if (Objects.equals(endpoint, that.endpoint) &&
                    Objects.equals(username, that.username) &&
                    Objects.equals(password, that.password)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("endpoint", endpoint)
                .add("username", username)
                .add("password", password)
                .toString();
    }
}
