/*
 * Copyright 2017-present Open Networking Laboratory
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
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.onlab.util.ImmutableByteSequence;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of an action, and its runtime parameters, of a table entry in a protocol-independent
 * pipeline.
 */
@Beta
public final class PiAction implements PiTableAction {

    private final PiActionId actionId;
    private final List<ImmutableByteSequence> runtimeParams;

    /**
     * Creates a new action instance for the given action identifier and runtime parameters.
     *
     * @param actionId      action identifier
     * @param runtimeParams list of runtime parameters
     */
    public PiAction(PiActionId actionId, List<ImmutableByteSequence> runtimeParams) {
        this.actionId = checkNotNull(actionId);
        this.runtimeParams = ImmutableList.copyOf(checkNotNull(runtimeParams));
    }

    /**
     * Creates a new action instance for the given action identifier, with no runtime parameters.
     *
     * @param actionId action identifier
     */
    public PiAction(PiActionId actionId) {
        this(actionId, Collections.emptyList());
    }

    @Override
    public Type type() {
        return Type.ACTION;
    }

    /**
     * Return the identifier of this action.
     *
     * @return action identifier
     */
    public PiActionId id() {
        return actionId;
    }

    /**
     * Returns an immutable view of the list of parameters of this action.
     * Return an empty list if the action doesn't take any runtime parameters.
     *
     * @return list of byte sequences
     */
    public List<ImmutableByteSequence> parameters() {
        return runtimeParams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiAction piAction = (PiAction) o;
        return Objects.equal(actionId, piAction.actionId) &&
                Objects.equal(runtimeParams, piAction.runtimeParams);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(actionId, runtimeParams);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.parameters().forEach(p -> stringParams.add(p.toString()));
        return MoreObjects.toStringHelper(this)
                .addValue(this.id().toString() + stringParams.toString())
                .toString();
    }
}
