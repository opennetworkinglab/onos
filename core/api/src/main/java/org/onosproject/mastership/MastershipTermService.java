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
package org.onosproject.mastership;

import org.onosproject.net.DeviceId;

// TODO give me a better name
/**
 * Service to obtain mastership term information.
 */
public interface MastershipTermService {

    // TBD: manage/increment per device mastership change
    //      or increment on any change
    /**
     * Returns the term number of mastership change occurred for given device.
     *
     * @param deviceId the identifier of the device
     * @return current master's term.
     */
    MastershipTerm getMastershipTerm(DeviceId deviceId);
}
