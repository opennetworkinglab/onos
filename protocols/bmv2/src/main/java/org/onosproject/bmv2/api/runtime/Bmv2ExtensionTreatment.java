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

import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

/**
 * Extension treatment for Bmv2 used as a wrapper for a {@link Bmv2Action}.
 */
public class Bmv2ExtensionTreatment extends AbstractExtension implements ExtensionTreatment {

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
        return ExtensionTreatmentType.ExtensionTreatmentTypes.P4_BMV2_ACTION.type();
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(action);
    }

    @Override
    public void deserialize(byte[] data) {
        action = appKryo.deserialize(data);
    }
}
