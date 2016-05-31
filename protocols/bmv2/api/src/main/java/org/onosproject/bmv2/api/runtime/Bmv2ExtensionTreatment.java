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

package org.onosproject.bmv2.api.runtime;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.BMV2_ACTION;

/**
 * Extension treatment for BMv2 used as a wrapper for a {@link Bmv2Action}.
 */
public final class Bmv2ExtensionTreatment extends AbstractExtension implements ExtensionTreatment {

    private final KryoNamespace appKryo = new KryoNamespace.Builder().build();
    private Bmv2Action action;

    public Bmv2ExtensionTreatment(Bmv2Action action) {
        this.action = action;
    }

    public Bmv2Action getAction() {
        return action;
    }

    @Override
    public ExtensionTreatmentType type() {
        return BMV2_ACTION.type();
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(action);
    }

    @Override
    public void deserialize(byte[] data) {
        action = appKryo.deserialize(data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(action);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ExtensionTreatment other = (Bmv2ExtensionTreatment) obj;
        return Objects.equal(this.action, other.action);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("action", action)
                .toString();
    }
}
