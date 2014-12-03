/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow;

import org.onosproject.net.PortNumber;

/**
 * Abstraction of different kinds of treatment that can be applied to an
 * outbound packet.
 */
public interface Treatment {

    // TODO: implement these later: modifications, group
    // TODO: elsewhere provide factory methods for some default treatments

    /**
     * Returns the port number where the packet should be emitted.
     *
     * @return output port number
     */
    PortNumber output();

}
