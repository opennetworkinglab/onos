/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Behaviour for handling various drivers for policer configurations.
 */
@Beta
public interface PolicerConfigurable extends HandlerBehaviour {

    /**
     * Allocates a new policer id. There may not be any correspondence with the identifiers
     * of the technology implementing the Policer in the device. Mapping (if necessary) is left
     * to the specific implementation.
     *
     * @return the policer id or {@link PolicerId#NONE} if there is a failure
     */
    PolicerId allocatePolicerId();

    /**
     * Free a policer id. There may not be any correspondence with the identifiers
     * of the technology implementing the Policer in the device. Mapping (if necessary) is left
     * to the specific implementation.
     *
     * @param id the policer id
     */
    void freePolicerId(PolicerId id);

    // Still WIP, throws NotImplementedException
    void addPolicer(Policer policer);

    // Still WIP, throws NotImplementedException
    void deletePolicer(PolicerId id);

    // Still WIP, throws NotImplementedException
    Policer getPolicer(PolicerId policerId);

    // Still WIP, throws NotImplementedException
    Collection<Policer> getPolicers();

}
