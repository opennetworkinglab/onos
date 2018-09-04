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
 * Representation of openstack keystone config information.
 */
public interface KeystoneConfig {

    /**
     * Returns the endpoint URL info.
     *
     * @return keystone authentication info
     */
    String endpoint();

    /**
     * Returns the keystone authentication info.
     *
     * @return keystone authentication info
     */
    OpenstackAuth authentication();

    /**
     * Builder of new keystone config entity.
     */
    interface Builder {

        /**
         * Builds an immutable keystone config instance.
         *
         * @return keystone config instance
         */
        KeystoneConfig build();

        /**
         * Returns keystone config builder with supplied endpoint.
         *
         * @param endpoint endpoint of keystone
         * @return keystone config builder
         */
        Builder endpoint(String endpoint);

        /**
         * Returns keystone config builder with supplied authentication info.
         *
         * @param auth authentication info
         * @return keystone config builder
         */
        Builder authentication(OpenstackAuth auth);
    }
}
