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
package org.onosproject.net.host;

import org.onosproject.net.HostId;
import org.onosproject.store.Timestamp;

/**
 * Interface for a logical clock service that issues per host timestamps.
 */
public interface HostClockService {

    /**
     * Returns a new timestamp for the specified host.
     * @param hostId identifier for the host.
     * @return timestamp.
     */
    Timestamp getTimestamp(HostId hostId);
}
