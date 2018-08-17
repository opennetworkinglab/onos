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
 * Implementation class of openstack authentication.
 */
public final class DefaultOpenstackAuth implements OpenstackAuth {

    private final String version;
    private final Protocol protocol;
    private final String username;
    private final String password;
    private final String project;
    private final Perspective perspective;

    private static final String NOT_NULL_MSG = "% cannot be null";

    /**
     * A default constructor of keystone authentication instance.
     *
     * @param version       version number
     * @param protocol      endpoint protocol type
     * @param username      keystone username
     * @param password      keystone password
     * @param project       project name
     * @param perspective   user perspective
     */
    private DefaultOpenstackAuth(String version, Protocol protocol,
                                   String username, String password, String project,
                                   Perspective perspective) {

        this.version = version;
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.project = project;
        this.perspective = perspective;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public String project() {
        return project;
    }

    @Override
    public Perspective perspective() {
        return perspective;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOpenstackAuth) {
            DefaultOpenstackAuth that = (DefaultOpenstackAuth) obj;
            return Objects.equals(version, that.version) &&
                    Objects.equals(protocol, that.protocol) &&
                    Objects.equals(username, that.username) &&
                    Objects.equals(password, that.password) &&
                    Objects.equals(project, that.project) &&
                    Objects.equals(perspective, that.perspective);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version,
                protocol,
                username,
                password,
                project,
                perspective);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("version", version)
                .add("protocol", protocol)
                .add("username", username)
                .add("password", password)
                .add("project", project)
                .add("perspective", perspective)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return keystone authentication instance builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for openstack authentication.
     */
    public static final class Builder implements OpenstackAuth.Builder {

        private String version;
        private Protocol protocol;
        private String username;
        private String password;
        private String project;
        private Perspective perspective;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public OpenstackAuth build() {
            checkArgument(version != null, NOT_NULL_MSG, "version");
            checkArgument(protocol != null, NOT_NULL_MSG, "protocol");
            checkArgument(username != null, NOT_NULL_MSG, "username");
            checkArgument(password != null, NOT_NULL_MSG, "password");
            checkArgument(project != null, NOT_NULL_MSG, "project");

            return new DefaultOpenstackAuth(version,
                    protocol,
                    username,
                    password,
                    project,
                    perspective);
        }

        @Override
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        @Override
        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        @Override
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        @Override
        public Builder project(String project) {
            this.project = project;
            return this;
        }

        @Override
        public Builder perspective(Perspective perspective) {
            this.perspective = perspective;
            return this;
        }
    }
}
