/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.driver.optical.flowrule;

import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;

/**
 * Interface for cross connects as common in optical networking.
 */
public interface CrossConnect extends FlowRule {
    /**
     * Returns the add/drop port of the cross connect.
     *
     * @return port number
     */
    PortNumber addDrop();

    /**
     * Returns the wavelength of the cross connect.
     *
     * @return OCh signal
     */
    OchSignal ochSignal();

    /**
     * Returns true if cross connect is adding traffic.
     *
     * @return true if add rule, false if drop rule
     */
    boolean isAddRule();
}
