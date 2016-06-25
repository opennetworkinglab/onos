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

package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Path Key SubObject: When a PCC needs to expand a path-key in order to expand a CPS, it
 * issues a Path Computation Request (PCReq) to the PCE identified in
 * the PKS in the RSVP-TE ERO that it is processing.  The PCC supplies
 * the PKS to be expanded in a PATH-KEY SubObject in the PCReq message.
 */
public class PathKeySubObject implements PcepValueType {

    /*
    Pathkey subobject(RFC 5520):
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |L|    Type     |     Length    |           Path-Key            |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                         PCE ID (4 bytes)                      |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PathKeySubObject.class);

    public static final byte TYPE = 0x40;
    public static final byte LENGTH = 8;
    private final short pathKey;
    private final int pceID;

    /**
     * Constructor for Path Key sub Object which initializes pathKey and pceId.
     *
     * @param pathKey path key provided by PCC
     * @param pceID ID for the PCE
     */
    public PathKeySubObject(short pathKey, int pceID) {
        this.pathKey = pathKey;
        this.pceID = pceID;
    }

    /**
     * Creates Path Key sub Object which initializes pathKey and pceId.
     *
     * @param pathKey path key provided by PCC
     * @param pceID PCE id
     * @return new object of type path key sub object
     */
    public static PathKeySubObject of(short pathKey, int pceID) {
        return new PathKeySubObject(pathKey, pceID);
    }

    /**
     * Returns Path Key.
     *
     * @return pathKey
     */
    public short getPathKey() {
        return pathKey;
    }

    /**
     * Returns pceID.
     *
     * @return pceID
     */
    public int getPceId() {
        return pceID;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathKey, pceID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PathKeySubObject) {
            PathKeySubObject other = (PathKeySubObject) obj;
            return Objects.equals(this.pathKey, other.pathKey) && Objects.equals(this.pceID, other.pceID);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);

        c.writeShort(pathKey);
        c.writeInt(pceID);

        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns new path key sub objects.
     *
     * @param c of type channel buffer
     * @return object of type path key sub object
     */
    public static PcepValueType read(ChannelBuffer c) {
        Short pathKey = c.readShort();
        int pceID = c.readInt();
        return new PathKeySubObject(pathKey, pceID);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("PathKey", pathKey)
                .add("PceID", pceID)
                .toString();
    }
}
