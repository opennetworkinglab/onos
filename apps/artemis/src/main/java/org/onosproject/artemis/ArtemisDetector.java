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
 * Interface for Detector Service of Artemis.
 *
 * The detection service combines the information received through the events generated from the monitor service and
 * the configuration file that includes all the legit BGP paths. The purpose of this interface is to identify given
 * a BGP update message if there is a BGP hijack or not.
 */
public interface ArtemisDetector {
    //TODO: give the ability to other services to check the legitimacy of a BGP Update message
}
