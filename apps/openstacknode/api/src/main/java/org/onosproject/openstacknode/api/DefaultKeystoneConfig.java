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

package org.onosproject.openstacknode.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation class of keystone config.
 */
public final class DefaultKeystoneConfig implements KeystoneConfig {

    private final String endpoint;
    private final OpenstackAuth auth;

    private static final String NOT_NULL_MSG = "% cannot be null";

    private DefaultKeystoneConfig(String endpoint, OpenstackAuth auth) {
        this.endpoint = endpoint;
        this.auth = auth;
    }

    @Override
    public String endpoint() {
        return endpoint;
    }

    @Override
    public OpenstackAuth authentication() {
        return auth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DefaultKeystoneConfig) {
            DefaultKeystoneConfig that = (DefaultKeystoneConfig) o;
            return Objects.equals(endpoint, that.endpoint) &&
                    Objects.equals(auth, that.auth);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hash(endpoint, auth);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("endpoint", endpoint)
                .add("auth", auth)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return keystone config instance builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for keystone config.
     */
    public static final class Builder implements KeystoneConfig.Builder {

        private String endpoint;
        private OpenstackAuth auth;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public KeystoneConfig build() {
            checkArgument(endpoint != null, NOT_NULL_MSG, "endpoint");
            checkArgument(auth != null, NOT_NULL_MSG, "auth");

            return new DefaultKeystoneConfig(endpoint, auth);
        }

        @Override
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Builder authentication(OpenstackAuth auth) {
            this.auth = auth;
            return this;
        }
    }
}
