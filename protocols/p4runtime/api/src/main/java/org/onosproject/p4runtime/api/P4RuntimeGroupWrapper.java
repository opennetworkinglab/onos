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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.net.group.Group;
import org.onosproject.net.pi.runtime.PiActionGroup;

/**
 * A wrapper for a ONOS group installed on a P4Runtime device.
 */
@Beta
public class P4RuntimeGroupWrapper {
    private final PiActionGroup piActionGroup;
    private final Group group;
    private final long installMilliSeconds;

    /**
     * Creates new group wrapper.
     *
     * @param piActionGroup the Pi action group
     * @param group the group
     * @param installMilliSeconds the installation time
     */
    public P4RuntimeGroupWrapper(PiActionGroup piActionGroup, Group group,
                                 long installMilliSeconds) {
        this.piActionGroup = piActionGroup;
        this.group = group;
        this.installMilliSeconds = installMilliSeconds;
    }

    /**
     * Gets PI action group from this wrapper.
     *
     * @return the PI action group
     */
    public PiActionGroup piActionGroup() {
        return piActionGroup;
    }

    /**
     * Gets group from this wrapper.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Gets installation time of this wrapper.
     *
     * @return the installation time
     */
    public long installMilliSeconds() {
        return installMilliSeconds;
    }
}
