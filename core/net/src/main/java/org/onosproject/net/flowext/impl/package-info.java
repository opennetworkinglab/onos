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

/**
 * Core subsystem for tracking and manipulating global extended flow state.
 * This module is still under development, this module is used for external 
 * application to generate openflow flowrule extension and use onos to route
 * the packet to device by deviceId. If we want to use these api, we should write an agent
 * on app layer to receive packet and parse common structure as an abstraction.
 */
package org.onosproject.net.flowext.impl;
