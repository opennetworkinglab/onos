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
package org.onosproject.net.resource;

/**
 * Represents no-op codec intended to used in an empty discrete resource set only.
 * It's not supposed to be used by other classes.
 */
public class NoOpCodec implements DiscreteResourceCodec {
    public static final DiscreteResourceCodec INSTANCE = new NoOpCodec();

    @Override
    public int encode(DiscreteResource resource) {
        return 0;
    }

    @Override
    public DiscreteResource decode(DiscreteResourceId parent, int value) {
        return Resource.ROOT;
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
        return NoOpCodec.class.hashCode();
    }
}
