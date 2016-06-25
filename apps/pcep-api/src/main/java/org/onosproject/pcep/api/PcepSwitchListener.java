/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.pcep.api;

/**
 * Notifies providers about switch in events.
 */
public interface PcepSwitchListener {

    /**
     * Notify that the switch was added.
     *
     * @param dpid the switch where the event occurred
     */
    void switchAdded(PcepDpid dpid);

    /**
     * Notify that the switch was removed.
     *
     * @param dpid the switch where the event occurred.
     */
    void switchRemoved(PcepDpid dpid);

    /**
     * Notify that the switch has changed in some way.
     *
     * @param dpid the switch that changed
     */
    void switchChanged(PcepDpid dpid);

}
