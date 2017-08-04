/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Behaviour for handling various drivers for mirroring configurations.
 */
@Beta
public interface MirroringConfig extends HandlerBehaviour {

    /**
     * Adds a mirroring with a given description.
     *
     * @param bridge the bridge name
     * @param mirroringDescription mirroring description
     * @return true if succeeds, or false
     */
    boolean addMirroring(BridgeName bridge, MirroringDescription mirroringDescription);

    /**
     * Removes a mirroring.
     *
     * @param mirroringName mirroring name
     */
    void deleteMirroring(MirroringName mirroringName);

    /**
     * Returns a collection of MirroringStatistics.
     *
     * @return statistics collection
     */
    Collection<MirroringStatistics> getMirroringStatistics();

}
