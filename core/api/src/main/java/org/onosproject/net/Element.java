/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.net.driver.Projectable;

/**
 * Base abstraction of a network element, i.e. an infrastructure device or an end-station host.
 */
public interface Element extends Annotated, Provided, Projectable {

    /**
     * Returns the network element identifier.
     *
     * @return element identifier
     */
    ElementId id();

}
