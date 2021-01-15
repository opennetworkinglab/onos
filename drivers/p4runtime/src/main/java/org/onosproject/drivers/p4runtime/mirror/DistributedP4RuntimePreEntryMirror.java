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

import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.runtime.PiPreEntryHandle;
import org.osgi.service.component.annotations.Component;

/**
 * Distributed implementation of a P4Runtime PRE entry mirror.
 */
@Component(immediate = true, service = P4RuntimePreEntryMirror.class)
public final class DistributedP4RuntimePreEntryMirror
        extends AbstractDistributedP4RuntimeMirror<PiPreEntryHandle, PiPreEntry>
        implements P4RuntimePreEntryMirror {

    public DistributedP4RuntimePreEntryMirror() {
        // PI does not support reading PRE entries. To avoid inconsistencies,
        // flush mirror on device disconnection and other events which
        // invalidate pipeline status.
        super(PiEntityType.PRE_ENTRY, true);
    }

    @Override
    protected String mapSimpleName() {
        return PiEntityType.PRE_ENTRY.name().toLowerCase();
    }
}
