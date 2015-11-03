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
package org.onosproject.openstackswitching;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * Handles configuration for OpenstackSwitching app.
 */
public class OpenstackSwitchingConfig extends Config<ApplicationId> {
    public static final String DONOTPUSH = "do_not_push_flows";

    /**
     * Returns the flag whether the app pushes flows or not.
     *
     * @return the flag or false if not set
     */
    public boolean doNotPushFlows() {
        String flag = get(DONOTPUSH, "false");
        return Boolean.valueOf(flag);
    }

    /**
     * Sets the flag whether the app pushes flows or not.
     *
     * @param flag the flag whether the app pushes flows or not
     * @return self
     */
    public BasicElementConfig doNotPushFlows(boolean flag) {
        return (BasicElementConfig) setOrClear(DONOTPUSH, flag);
    }
}
