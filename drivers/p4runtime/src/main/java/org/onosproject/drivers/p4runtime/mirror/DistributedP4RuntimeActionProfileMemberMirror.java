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

package org.onosproject.drivers.p4runtime.mirror;

import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberHandle;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.osgi.service.component.annotations.Component;

/**
 * Distributed implementation of a P4Runtime action profile member mirror.
 */
@Component(immediate = true, service = P4RuntimeActionProfileMemberMirror.class)
public class DistributedP4RuntimeActionProfileMemberMirror
        extends AbstractDistributedP4RuntimeMirror
        <PiActionProfileMemberHandle, PiActionProfileMember>
        implements P4RuntimeActionProfileMemberMirror {

    public DistributedP4RuntimeActionProfileMemberMirror() {
        super(PiEntityType.ACTION_PROFILE_MEMBER);
    }

    @Override
    protected String mapSimpleName() {
        return PiEntityType.ACTION_PROFILE_MEMBER.name().toLowerCase();
    }
}
