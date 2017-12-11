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
package org.onosproject.incubator.net.l2monitoring.cfm.service;

import com.google.common.base.MoreObjects;
import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;

import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Event related to the maintenance of CFM MDs.
 */
public class MdEvent extends AbstractEvent<MdEvent.Type, MdId> {

    private MaIdShort maId;
    private MaintenanceDomain oldMd;
    /**
     * MD Event types supported.
     */
    public enum Type {
        MD_ADDED,
        MD_REMOVED,
        MD_UPDATED,
        MA_ADDED,
        MA_REMOVED
    }

    public MdEvent(Type type, MdId mdId) {
        super(type, mdId);
    }

    /**
     * Constructor that allows the MD to be held in the event.
     * This is useful if the MD had been deleted - it will be the only way of
     * retrieving some of its attributes
     * @param type The type of the event
     * @param mdId The ID of the MD
     * @param md The whole MD
     * @throws CfmConfigException if there's a problem copying MD
     */
    public MdEvent(Type type, MdId mdId, MaintenanceDomain md) throws CfmConfigException {
        super(type, mdId);
        this.oldMd = DefaultMaintenanceDomain.builder(md).build();
    }

    public MdEvent(Type type, MdId mdId, MaintenanceDomain md, MaIdShort maId)
            throws CfmConfigException {
        super(type, mdId);
        this.maId = maId;
        this.oldMd = DefaultMaintenanceDomain.builder(md).build();
    }

    public Optional<MaIdShort> maId() {
        return maId == null ? Optional.empty() : Optional.of(maId);
    }

    public Optional<MaintenanceDomain> md() {
        return oldMd == null ? Optional.empty() : Optional.of(oldMd);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("subject", subject());
        if (maId != null) {
            helper = helper.add("subject2", maId);
        }
        return helper.toString();
    }
}
