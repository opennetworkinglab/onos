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
package org.onosproject.drivers.fujitsu.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Device behaviour to obtain and set alert filter in vOLT.
 * Device behaviour to subscribe to receive notifications from vOLT.
 */
@Beta
public interface VoltAlertConfig extends HandlerBehaviour {

    /**
     * Get alert filter severity level.
     *
     * @return response string
     */
    String getAlertFilter();

    /**
     * Set alert filter severity level.
     *
     * @param severity input data in string
     * @return true if success
     */
    boolean setAlertFilter(String severity);

    /**
     * Subscribe to receive notifications or unsubscribe.
     *
     * @param mode disable subscription
     * @return true if success
     */
    boolean subscribe(String mode);
}
