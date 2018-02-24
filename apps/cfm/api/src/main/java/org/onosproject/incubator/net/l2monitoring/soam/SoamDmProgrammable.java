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
package org.onosproject.incubator.net.l2monitoring.soam;

import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Behaviour that allows Layer 2 SOAM PM in the form of Delay Measurement to be implemented by devices.
 *
 * Has all of the same methods as {@link SoamService},
 * so we don't repeat them here
 */
public interface SoamDmProgrammable extends HandlerBehaviour, SoamService {
}
