/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.store.meter.impl;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFailReason;

import java.util.Optional;

/**
 * A class representing the meter information stored in the meter store.
 */
public class MeterData {

    private final Meter meter;
    private final Optional<MeterFailReason> reason;
    private final NodeId origin;

    public MeterData(Meter meter, MeterFailReason reason, NodeId origin) {
        this.meter = meter;
        this.reason = Optional.ofNullable(reason);
        this.origin = origin;
    }

    public Meter meter() {
        return meter;
    }

    public Optional<MeterFailReason> reason() {
        return this.reason;
    }

    public NodeId origin() {
        return this.origin;
    }


}
