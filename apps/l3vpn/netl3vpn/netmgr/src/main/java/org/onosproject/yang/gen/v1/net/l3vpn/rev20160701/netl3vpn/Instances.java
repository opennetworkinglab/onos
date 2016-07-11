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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn;

import java.util.List;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.Instance;

/**
 * Abstraction of an entity which represents the functionality of instances.
 */
public interface Instances {

    /**
     * Returns the attribute instance.
     *
     * @return list of instance
     */
    List<Instance> instance();

    /**
     * Builder for instances.
     */
    interface InstancesBuilder {

        /**
         * Returns the attribute instance.
         *
         * @return list of instance
         */
        List<Instance> instance();

        /**
         * Returns the builder object of instance.
         *
         * @param instance list of instance
         * @return builder object of instance
         */
        InstancesBuilder instance(List<Instance> instance);

        /**
         * Builds object of instances.
         *
         * @return object of instances.
         */
        Instances build();
    }
}