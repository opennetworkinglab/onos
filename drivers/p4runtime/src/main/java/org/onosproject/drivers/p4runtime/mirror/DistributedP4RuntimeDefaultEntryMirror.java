/*
 * Copyright 2019-present Open Networking Foundation
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
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableEntryHandle;
import org.osgi.service.component.annotations.Component;

/**
 * Distributed implementation of a P4Runtime default entry mirror.
 */
@Component(immediate = true, service = P4RuntimeDefaultEntryMirror.class)
public final class DistributedP4RuntimeDefaultEntryMirror
        extends AbstractDistributedP4RuntimeMirror
                        <PiTableEntryHandle, PiTableEntry>
        implements P4RuntimeDefaultEntryMirror {

    public DistributedP4RuntimeDefaultEntryMirror() {
        super(PiEntityType.TABLE_ENTRY);
    }

    @Override
    protected String mapSimpleName() {
        return PiEntityType.TABLE_ENTRY.name().toLowerCase() + "-default";
    }
}
