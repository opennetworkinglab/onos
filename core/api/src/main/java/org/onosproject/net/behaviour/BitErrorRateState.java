/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This Work is contributed by Sterlite Technologies
 */
package org.onosproject.net.behaviour;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Optional;

/*
 * Interface to compute BitErrorRate(BER) value pre and post Forward Error Correction (FEC).
 * BER is dependent on FEC value
 */
public interface BitErrorRateState extends HandlerBehaviour {

    /**
     * Get the BER value pre FEC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @return the decimal value of BER
     */
    Optional<Double> getPreFecBer(DeviceId deviceId, PortNumber port);

    /**
     * Get the BER value post FEC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @return the decimal value of BER
     */
    Optional<Double> getPostFecBer(DeviceId deviceId, PortNumber port);
}
