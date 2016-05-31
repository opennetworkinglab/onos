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

package org.onosproject.bmv2.api.context;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A BMv2 device context, defined by a configuration and an interpreter.
 */
@Beta
public final class Bmv2DeviceContext {

    private final Bmv2Configuration configuration;
    private final Bmv2Interpreter interpreter;

    /**
     * Creates a new BMv2 device context.
     *
     * @param configuration a configuration
     * @param interpreter   an interpreter
     */
    public Bmv2DeviceContext(Bmv2Configuration configuration, Bmv2Interpreter interpreter) {
        this.configuration = checkNotNull(configuration, "configuration cannot be null");
        this.interpreter = checkNotNull(interpreter, "interpreter cannot be null");
    }

    /**
     * Returns the BMv2 configuration of this context.
     *
     * @return a configuration
     */
    public Bmv2Configuration configuration() {
        return configuration;
    }

    /**
     * Returns the BMv2 interpreter of this context.
     *
     * @return an interpreter
     */
    public Bmv2Interpreter interpreter() {
        return interpreter;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(configuration, interpreter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2DeviceContext other = (Bmv2DeviceContext) obj;
        return Objects.equal(this.configuration, other.configuration)
                && Objects.equal(this.interpreter, other.interpreter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("configuration", configuration)
                .add("interpreter", interpreter)
                .toString();
    }
}
