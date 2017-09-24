/*
 * Copyright 2015-present Open Networking Foundation
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
 * A representation of a meter cell identifier. Uniquely identifies a meter cell
 * in the scope of a single device.
 * <p>
 * This ID uniquely identifies a meter cell within in a switch that maintains
 * only one meter instance. If a switch supports multiple meter instances (like
 * in P4), then {@link org.onosproject.net.pi.runtime.PiMeterCellId} should be
 * used. In this case, meter cells are defined starting with id=1 up to the
 * maximum number of cells that the switch can support. The OpenFlow protocol
 * also defines some additional virtual meter cells that can not be associated
 * with flows.
 */
public final class MeterId extends Identifier<Long> implements MeterCellId {

    // TODO: should rename this class to SimpleMeterCellId to distinguish it
    // from PiMeterId and PiMeterCellId. From ONOS-7051, to follow the P4
    // abstraction there can be multiple instances of a meter in a data plane,
    // each meter instance is made of multiple cells. This class is based on the
    // OpenFlow abstraction where, following P4 terminology, the data plane
    // maintains only one meter instance. What is described here as a MeterId is
    // indeed the identifier of a meter cell.

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

    @Override
    public MeterCellType type() {
        return MeterCellType.INDEX;
    }
}
