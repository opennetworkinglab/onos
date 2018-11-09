/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual;

import java.util.Comparator;

/**
 * Various comparators.
 */
public final class Comparators {

    // Ban construction
    private Comparators() {
    }

    public static final Comparator<VirtualNetwork> VIRTUAL_NETWORK_COMPARATOR =
            (v1, v2) -> {
                int compareId = v1.tenantId().toString().compareTo(v2.tenantId().toString());
                return (compareId != 0) ? compareId : Long.signum(v1.id().id() - v2.id().id());
            };

    public static final Comparator<VirtualDevice> VIRTUAL_DEVICE_COMPARATOR =
            (v1, v2) -> v1.id().toString().compareTo(v2.id().toString());

    public static final Comparator<VirtualPort> VIRTUAL_PORT_COMPARATOR =
            (v1, v2) -> v1.number().toString().compareTo(v2.number().toString());

}
