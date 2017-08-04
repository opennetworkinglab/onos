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
package org.onosproject.net.link;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Description;
import org.onosproject.net.Link;

/**
 * Describes an infrastructure link.
 */
public interface LinkDescription extends Description {

    /**
     * Returns the link source.
     *
     * @return links source
     */
    ConnectPoint src();

    /**
     * Returns the link destination.
     *
     * @return links destination
     */
    ConnectPoint dst();

    /**
     * Returns the link type.
     *
     * @return link type
     */
    Link.Type type();

    /**
     * Returns true if the link is expected, false otherwise.
     *
     * @return expected flag
     */
    boolean isExpected();

    // Add further link attributes
}
