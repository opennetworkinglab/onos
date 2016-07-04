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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.nes;

/**
 * Abstraction of an entity which represents the functionality of ne.
 */
public interface Ne {

    /**
     * Returns the attribute id.
     *
     * @return value of id
     */
    String id();

    /**
     * Builder for ne.
     */
    interface NeBuilder {

        /**
         * Returns the attribute id.
         *
         * @return value of id
         */
        String id();

        /**
         * Returns the builder object of id.
         *
         * @param id value of id
         * @return builder object of id
         */
        NeBuilder id(String id);

        /**
         * Builds object of ne.
         *
         * @return object of ne.
         */
        Ne build();
    }
}