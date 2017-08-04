/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

/**
 * Representation of a relationship role of a controller instance to a device
 * or a region of network environment.
 */
public enum MastershipRole {

    /**
     * Represents a relationship where the controller instance is the master
     * to a device or a region of network environment.
     */
    MASTER,

    /**
     * Represents a relationship where the controller instance is the standby,
     * i.e. potential master to a device or a region of network environment.
     */
    STANDBY,

    /**
     * Represents that the controller instance is not eligible to be the master
     * to a device or a region of network environment.
     */
    NONE

}
