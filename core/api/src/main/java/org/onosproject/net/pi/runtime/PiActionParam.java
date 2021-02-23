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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiActionParamId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Instance of an action runtime parameter in a match+action table of a
 * protocol-independent pipeline.
 */
@Beta
public final class PiActionParam {

    private final PiActionParamId id;
    private final ImmutableByteSequence value;

    /**
     * Creates an action's runtime parameter for the given param ID and byte
     * sequence value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, ImmutableByteSequence value) {
        this.id = checkNotNull(id);
        this.value = checkNotNull(value);
        checkArgument(value.size() > 0, "Value can't have size 0");
    }

    /**
     * Creates an action's runtime parameter for the given param ID and byte
     * value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, byte value) {
        this(id, copyFrom(value));
    }

    /**
     * Creates an action's runtime parameter for the given param ID and short
     * value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, short value) {
        this(id, copyFrom(value));
    }

    /**
     * Creates an action's runtime parameter for the given param ID and int
     * value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, int value) {
        this(id, copyFrom(value));
    }

    /**
     * Creates an action's runtime parameter for the given param ID and long
     * value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, long value) {
        this(id, copyFrom(value));
    }

    /**
     * Creates an action's runtime parameter for the given param ID and byte
     * array value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, byte[] value) {
        this(id, copyFrom(value));
    }

    /**
     * Creates an action's runtime parameter for the given param ID and string
     * value.
     *
     * @param id    parameter identifier
     * @param value value
     */
    public PiActionParam(PiActionParamId id, String value) {
        this(id, copyFrom(value));
    }

    /**
     * Returns the identifier of this parameter.
     *
     * @return parameter identifier
     */
    public PiActionParamId id() {
        return id;
    }

    /**
     * Returns the value of this parameter.
     *
     * @return parameter value
     */
    public ImmutableByteSequence value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiActionParam that = (PiActionParam) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, value);
    }

    @Override
    public String toString() {
        return this.id.toString() + "=" + this.value().toString();
    }
}
