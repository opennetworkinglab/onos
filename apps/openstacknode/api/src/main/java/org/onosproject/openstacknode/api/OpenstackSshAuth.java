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
 * Representation of ssh authentication information for node.
 */
public interface OpenstackSshAuth {
    /**
     * Returns the ID for ssh authentication.
     *
     * @return id
     */
    String id();

    /**
     * Returns the password for ssh authentication.
     *
     * @return password
     */
    String password();

    /**
     * Builder of OpenstackSshAuth instance.
     */
    interface Builder {
        /**
         * Builds an immutable OpenstackSshAuth instance.
         *
         * @return OpenstackSsshAuth instance
         */
        OpenstackSshAuth build();

        /**
         * Returns OpenstackSshAuth builder with supplied ID.
         *
         * @param id id
         * @return OpenstackSsshAuth builder
         */
        Builder id(String id);

        /**
         * Returns OpenstackSshAuth builder with supplied password.
         *
         * @param password password
         * @return OpenstackSsshAuth builder
         */
        Builder password(String password);
    }
}
