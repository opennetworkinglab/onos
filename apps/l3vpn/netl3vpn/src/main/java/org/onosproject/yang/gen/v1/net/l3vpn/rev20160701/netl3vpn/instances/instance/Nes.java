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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance;

import java.util.List;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.nes.Ne;

/**
 * Abstraction of an entity which represents the functionality of nes.
 */
public interface Nes {

    /**
     * Returns the attribute ne.
     *
     * @return list of ne
     */
    List<Ne> ne();

    /**
     * Builder for nes.
     */
    interface NesBuilder {

        /**
         * Returns the attribute ne.
         *
         * @return list of ne
         */
        List<Ne> ne();

        /**
         * Returns the builder object of ne.
         *
         * @param ne list of ne
         * @return builder object of ne
         */
        NesBuilder ne(List<Ne> ne);

        /**
         * Builds object of nes.
         *
         * @return object of nes.
         */
        Nes build();
    }
}