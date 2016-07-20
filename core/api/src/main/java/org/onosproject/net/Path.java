/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net;

import java.util.List;

/**
 * Representation of a contiguous directed path in a network. Path comprises
 * of a sequence of links, where adjacent links must share the same device,
 * meaning that destination of the source of one link must coincide with the
 * destination of the previous link.
 */
public interface Path extends Link {

    /**
     * Returns sequence of links comprising the path.
     *
     * @return list of links
     */
    List<Link> links();

    /**
     * Returns the path cost as a unit-less value.
     *
     * @return unit-less path cost
     */
    double cost();

}
