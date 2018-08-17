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

/**
 * Representation of keystone authentication information.
 */
public interface OpenstackAuth {

    /**
     * Keystone authentication protocol types.
     */
    enum Protocol {
        HTTP,
        HTTPS
    }

    /**
     * Keystone user perspective.
     */
    enum Perspective {
        ADMIN,
        INTERNAL,
        PUBLIC
    }

    /**
     * Returns the keystone authentication version number.
     *
     * @return keystone authentication version
     */
    String version();

    /**
     * Returns the keystone authentication protocol type.
     *
     * @return keystone authentication protocol type
     */
    Protocol protocol();

    /**
     * Returns the keystone username.
     *
     * @return keystone username
     */
    String username();

    /**
     * Returns the keystone user password.
     *
     * @return keystone password
     */
    String password();

    /**
     * Returns the project name.
     *
     * @return project name
     */
    String project();

    /**
     * Returns the user perspective.
     *
     * @return user perspective
     */
    Perspective perspective();

    /**
     * Builder of keystone authentication info.
     */
    interface Builder {

        /**
         * Builds an immutable openstack keystone authentication instance.
         *
         * @return keystone authentication instance
         */
        OpenstackAuth build();

        /**
         * Returns keystone authentication builder with supplied version number.
         *
         * @param version version number
         * @return keystone authentication builder
         */
        Builder version(String version);

        /**
         * Returns keystone authentication builder with supplied protocol.
         *
         * @param protocol protocol
         * @return keystone authentication builder
         */
        Builder protocol(Protocol protocol);

        /**
         * Returns keystone authentication builder with supplied username.
         *
         * @param username username
         * @return keystone authentication builder
         */
        Builder username(String username);

        /**
         * Returns keystone authentication builder with supplied password.
         *
         * @param password password
         * @return keystone authentication builder
         */
        Builder password(String password);

        /**
         * Returns keystone authentication builder with supplied project.
         *
         * @param project project name
         * @return keystone authentication builder
         */
        Builder project(String project);

        /**
         * Returns keystone authentication builder with supplied perspective.
         *
         * @param perspective perspective
         * @return keystone authentication builder
         */
        Builder perspective(Perspective perspective);

    }
}
