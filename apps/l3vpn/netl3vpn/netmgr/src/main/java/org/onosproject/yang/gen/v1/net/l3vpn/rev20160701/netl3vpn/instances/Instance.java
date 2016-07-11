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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances;

import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.Acs;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.Nes;

/**
 * Abstraction of an entity which represents the functionality of instance.
 */
public interface Instance {

    /**
     * Returns the attribute id.
     *
     * @return value of id
     */
    String id();

    /**
     * Returns the attribute name.
     *
     * @return value of name
     */
    String name();

    /**
     * Returns the attribute mode.
     *
     * @return value of mode
     */
    String mode();

    /**
     * Returns the attribute nes.
     *
     * @return value of nes
     */
    Nes nes();

    /**
     * Returns the attribute acs.
     *
     * @return value of acs
     */
    Acs acs();

    /**
     * Builder for instance.
     */
    interface InstanceBuilder {

        /**
         * Returns the attribute id.
         *
         * @return value of id
         */
        String id();

        /**
         * Returns the attribute name.
         *
         * @return value of name
         */
        String name();

        /**
         * Returns the attribute mode.
         *
         * @return value of mode
         */
        String mode();

        /**
         * Returns the attribute nes.
         *
         * @return value of nes
         */
        Nes nes();

        /**
         * Returns the attribute acs.
         *
         * @return value of acs
         */
        Acs acs();

        /**
         * Returns the builder object of id.
         *
         * @param id value of id
         * @return builder object of id
         */
        InstanceBuilder id(String id);

        /**
         * Returns the builder object of name.
         *
         * @param name value of name
         * @return builder object of name
         */
        InstanceBuilder name(String name);

        /**
         * Returns the builder object of mode.
         *
         * @param mode value of mode
         * @return builder object of mode
         */
        InstanceBuilder mode(String mode);

        /**
         * Returns the builder object of nes.
         *
         * @param nes value of nes
         * @return builder object of nes
         */
        InstanceBuilder nes(Nes nes);

        /**
         * Returns the builder object of acs.
         *
         * @param acs value of acs
         * @return builder object of acs
         */
        InstanceBuilder acs(Acs acs);

        /**
         * Builds object of instance.
         *
         * @return object of instance.
         */
        Instance build();
    }
}