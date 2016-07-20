/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.resource.impl;

final class MapNames {
    static final String DISCRETE_CONSUMER_MAP = "onos-discrete-consumers";
    static final String DISCRETE_CHILD_MAP = "onos-resource-discrete-children";
    static final String CONTINUOUS_CONSUMER_MAP = "onos-continuous-consumers";
    static final String CONTINUOUS_CHILD_MAP = "onos-resource-continuous-children";

    // prohibit contruction
    private MapNames() {}
}
