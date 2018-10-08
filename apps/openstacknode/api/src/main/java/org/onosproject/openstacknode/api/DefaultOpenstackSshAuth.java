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
 * Implementation class of ssh authentication.
 */
public final class DefaultOpenstackSshAuth implements OpenstackSshAuth {
    private final String id;
    private final String password;

    private static final String NOT_NULL_MSG = "% cannot be null";

    /**
     * A default constructor of ssh authentication instance.
     *
     * @param id ssh auth ID
     * @param password ssh auth password
     */
    private DefaultOpenstackSshAuth(String id, String password) {
        this.id = id;
        this.password = password;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOpenstackSshAuth) {
            DefaultOpenstackSshAuth that = (DefaultOpenstackSshAuth) obj;
            return Objects.equals(id, that.id) &&
                    Objects.equals(password, that.password);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, password);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("password", password)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return ssh authentication instance builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of OpenstackSshAuth instance.
     */
    public static final class Builder implements OpenstackSshAuth.Builder {
        private String id;
        private String password;

        private Builder() {

        }

        @Override
        public OpenstackSshAuth build() {
            checkArgument(id != null, NOT_NULL_MSG, "id");
            checkArgument(password != null, NOT_NULL_MSG, "password");

            return new DefaultOpenstackSshAuth(id, password);
        }

        @Override
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder password(String password) {
            this.password = password;
            return this;
        }
    }
}
