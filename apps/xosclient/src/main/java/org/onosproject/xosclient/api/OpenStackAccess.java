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

/**
 * Object holding OpenStack API access information.
 */
// TODO remove this when XOS provides this information
public class OpenStackAccess {

    private final String endpoint;
    private final String tenant;
    private final String user;
    private final String password;

    /**
     * Default constructor.
     *
     * @param endpoint Keystone endpoint
     * @param tenant tenant name
     * @param user user name
     * @param password password
     */
    public OpenStackAccess(String endpoint, String tenant, String user, String password) {
        this.endpoint = endpoint;
        this.tenant = tenant;
        this.user = user;
        this.password = password;
    }

    /**
     * Returns OpenStack API endpoint.
     *
     * @return endpoint
     */
    public String endpoint() {
        return this.endpoint;
    }

    /**
     * Returns OpenStack tenant name.
     *
     * @return tenant name
     */
    public String tenant() {
        return this.tenant;
    }

    /**
     * Returns OpenStack user.
     *
     * @return user name
     */
    public String user() {
        return this.user;
    }

    /**
     * Returns OpenStack password for the user.
     *
     * @return password
     */
    public String password() {
        return this.password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, tenant, user, password);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj instanceof OpenStackAccess)) {
            OpenStackAccess that = (OpenStackAccess) obj;
            if (Objects.equals(endpoint, that.endpoint) &&
                    Objects.equals(tenant, that.tenant) &&
                    Objects.equals(user, that.user) &&
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
                .add("tenant", tenant)
                .add("user", user)
                .add("password", password)
                .toString();
    }
}
