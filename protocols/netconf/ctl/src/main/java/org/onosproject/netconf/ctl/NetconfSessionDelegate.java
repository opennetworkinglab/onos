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

package org.onosproject.netconf.ctl;

import org.onosproject.netconf.NetconfDeviceOutputEvent;

/**
 * Entity associated with a NetconfSessionImpl and capable of receiving notifications of
 * events about the session.
 *
 * @deprecated in 1.10.0
 */
@Deprecated
public interface NetconfSessionDelegate {

        /**
         * Notifies the delegate via the specified event.
         *
         * @param event store generated event
         */
        void notify(NetconfDeviceOutputEvent event);
}
