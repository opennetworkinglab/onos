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
import org.onlab.util.Identifier;

/**
 * Identifier of a member of an action profile in a protocol-independent
 * pipeline, unique within the scope on an action profile.
 */
@Beta
public final class PiActionProfileMemberId extends Identifier<Integer>
        implements PiTableAction {

    private PiActionProfileMemberId(int id) {
        super(id);
    }

    /**
     * Returns a member identifier for the given integer value.
     *
     * @param id identifier
     * @return action profile group
     */
    public static PiActionProfileMemberId of(int id) {
        return new PiActionProfileMemberId(id);
    }

    /*
    In P4Runtime, action profile members can be referenced directly as table
    actions. In future we should consider having a more appropriate wrapper
    class for group member IDs, instead of implementing the PiTableAction
    interface.
     */
    @Override
    public Type type() {
        return Type.ACTION_PROFILE_MEMBER_ID;
    }
}
