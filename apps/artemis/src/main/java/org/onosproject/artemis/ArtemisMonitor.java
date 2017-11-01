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
package org.onosproject.artemis;

/**
 * Interface for Monitor Service of Artemis.
 *
 * The monitoring service runs continuously and provides control plane information from the AS itself, the streaming
 * services can be RIPE RIS, BGPstream, BGPmon and Periscope, which return almost real-time BGP updates for a given
 * list of prefixes and ASNs. The purpose of this interface is to provide store and provide this BGO information to the
 * consumers (e.g. Artemis Detector Service).
 */
public interface ArtemisMonitor {
    //TODO: give access to BGP Update messages to other services through this service
}
