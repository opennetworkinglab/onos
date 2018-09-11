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

package org.onosproject.net.flow.instructions;


import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiTableAction;

/**
 * Representation of a protocol-independent instruction.
 */
@Beta
public final class PiInstruction implements Instruction {

    private final PiTableAction tableAction;

    /**
     * Creates a new instruction with the given protocol-independent table action.
     *
     * @param tableAction a protocol-independent action
     */
    PiInstruction(PiTableAction tableAction) {
        this.tableAction = tableAction;
    }

    /**
     * Returns the protocol-independent table action defined by this instruction.
     *
     * @return an action
     */
    public PiTableAction action() {
        return tableAction;
    }

    @Override
    public Type type() {
        return Type.PROTOCOL_INDEPENDENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiInstruction that = (PiInstruction) o;
        return Objects.equal(tableAction, that.tableAction);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tableAction);
    }

    @Override
    public String toString() {
        switch (tableAction.type()) {
            case ACTION_PROFILE_GROUP_ID:
                return "GROUP:0x" + Integer.toHexString(((PiActionProfileGroupId) tableAction).id());
            case ACTION_PROFILE_MEMBER_ID:
                return "GROUP_MEMBER:0x" + Integer.toHexString(((PiActionProfileMemberId) tableAction).id());
            default:
                return tableAction.toString();
        }
    }
}
