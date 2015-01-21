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
package org.onosproject.igp.controller.driver;

import org.onosproject.igp.controller.IgpDpid;

/**
 * Switch factory which returns concrete switch objects for the
 * physical openflow switch in use.
 *
 */
public interface IgpSwitchDriverFactory {


    /**
     * Constructs the real openflow switch representation.
     * @param dpid the dpid for this switch.
     * @param desc its description.
     * @param ofv the OF version in use
     * @return the openflow switch representation.
     */
    public IgpSwitchDriver getOFSwitchImpl(IgpDpid dpid);
}
