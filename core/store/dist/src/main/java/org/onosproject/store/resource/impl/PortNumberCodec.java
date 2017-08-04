/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.net.PortNumber;
import org.onosproject.net.resource.DiscreteResourceCodec;

class PortNumberCodec implements DiscreteResourceCodec<PortNumber> {
    @Override
    public int encode(PortNumber resource) {
        return (int) resource.toLong();
    }

    @Override
    public PortNumber decode(int value) {
        return PortNumber.portNumber(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return PortNumber.class.hashCode();
    }
}
