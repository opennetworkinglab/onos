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
package org.onosproject.net.meter;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A representation of a meter identifier.
 * Uniquely identifies a meter in the scope of a single device.
 * <p>
 * The meter_id field uniquely identifies a meter within a switch. Meters are
 * defined starting with meter_id=1 up to the maximum number of meters that the
 * switch can support. The OpenFlow protocol also defines some additional
 * virtual meters that can not be associated with flows:
 */
public final class MeterId extends Identifier<Long> {

    /**  Flow meters can use any number up to MAX. */
    public static final long MAX = 0xFFFF0000L;


    /* The following are virtual meters as defined in openflow-spec-1.3 P. 58 */
    /** Meter for slow datapath, if any. */
    public static final MeterId SLOWPATH = new MeterId(0xFFFFFFFDL);
    /** Meter for controller connection. */
    public static final MeterId CONTROLLER = new MeterId(0xFFFFFFFEL);
    /** Represents all meters for stat requests commands. */
    public static final MeterId ALL = new MeterId(0xFFFFFFFFL);


    private MeterId(long id) {
        super(id);
    }

    @Override
    public String toString() {
        return Long.toHexString(identifier);
    }

    /**
     * Creates a new meter identifier.
     *
     * @param id the backing identifier value
     * @return meter identifier
     */
    public static MeterId meterId(long id) {
        checkArgument(id > 0, "id cannot be negative nor 0");
        checkArgument(id <= MAX, "id cannot be larger than {}", MAX);
        return new MeterId(id);
    }
}
