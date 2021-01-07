/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import java.util.Set;

/**
 * Interface of kubevirt instance.
 */
public interface KubevirtInstance {

    /**
     * Returns the kubevirt instance UID.
     *
     * @return kubevirt instance UID
     */
    String uid();

    /**
     * Returns the kubevirt instance name.
     *
     * @return kubevirt instance name
     */
    String name();

    /**
     * Returns the kubevirt ports associated with the instance.
     *
     * @return kubevirt ports
     */
    Set<KubevirtPort> ports();

    interface Builder {

        /**
         * Builds on immutable instance.
         *
         * @return kubevirt instance
         */
        KubevirtInstance build();

        /**
         * Returns instance builder with supplied UID.
         *
         * @param uid UID of instance
         * @return instance builder
         */
        Builder uid(String uid);

        /**
         * Returns instance builder with supplied name.
         *
         * @param name name of instance
         * @return instance builder
         */
        Builder name(String name);

        /**
         * Returns instance builder with supplied ports.
         *
         * @param ports set of ports
         * @return instance builder
         */
        Builder ports(Set<KubevirtPort> ports);
    }
}
