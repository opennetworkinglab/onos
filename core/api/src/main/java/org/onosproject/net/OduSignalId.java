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
package org.onosproject.net;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.Objects;

import org.onlab.util.HexString;

import com.google.common.base.MoreObjects;
/**
 * Implementation of ODU Signal ID.
 *
 * <p>
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)".
 * </p>
 */
public class OduSignalId {

    private final int tributaryPortNumber;     // Tributary Port number
    private final int tributarySlotLength;        // Number of Tributary Slots included in tsmap
    private final byte[] tributarySlotBitmap; // Tributary slot bitmap

    public static final int TRIBUTARY_SLOT_BITMAP_SIZE = 10;

    /**
     * Creates an instance with the specified arguments.
     *
     * @param tributaryPortNumber   tributary port number
     * @param tributarySlotLen      tributary slot len
     * @param tributarySlotBitmap   tributary slot bitmap
     */
    public OduSignalId(int tributaryPortNumber, int tributarySlotLen,
            byte[] tributarySlotBitmap) {

        checkArgument(tributaryPortNumber <= 80,
                "tributaryPortNumber %s must be <= 80 ",
                 tributaryPortNumber);

        checkArgument(tributarySlotBitmap.length == TRIBUTARY_SLOT_BITMAP_SIZE,
                "number of elements in list " + HexString.toHexString(tributarySlotBitmap)
                + " must be equal to " + TRIBUTARY_SLOT_BITMAP_SIZE);

        checkArgument(tributarySlotLen <= 80,
                "tributarySlotLen %s must be <= 80 ",
                tributarySlotLen);

        this.tributaryPortNumber = tributaryPortNumber;
        this.tributarySlotLength = tributarySlotLen;
        this.tributarySlotBitmap = Arrays.copyOf(tributarySlotBitmap, tributarySlotBitmap.length);
    }

    /**
     * Returns the OduSignalId representing the specified parameters.
     *
     * @param tributaryPortNumber   tributary port number
     * @param tributarySlotLen      tributary slot len
     * @param tributarySlotBitmap   tributary slot bitmap
     * @return OduSignalId
     */
    public static OduSignalId oduSignalId(int tributaryPortNumber, int tributarySlotLen,
            byte[] tributarySlotBitmap) {
        return new OduSignalId(tributaryPortNumber, tributarySlotLen, tributarySlotBitmap);
    }


    /**
     * Returns tributary port number.
     *
     * @return the tributaryPortNumber
     */
    public int tributaryPortNumber() {
        return tributaryPortNumber;
    }

    /**
     * Returns tributary slot length.
     *
     * @return the tributarySlotLen
     */
    public int tributarySlotLength() {
        return tributarySlotLength;
    }

    /**
     * Returns tributary slot bitmap.
     *
     * @return the tributarySlotBitmap
     */
    public byte[] tributarySlotBitmap() {
        return Arrays.copyOf(tributarySlotBitmap, tributarySlotBitmap.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tributaryPortNumber, tributarySlotLength, Arrays.hashCode(tributarySlotBitmap));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OduSignalId)) {
            return false;
        }
        final OduSignalId other = (OduSignalId) obj;
        return   Objects.equals(this.tributaryPortNumber, other.tributaryPortNumber)
                 && Objects.equals(this.tributarySlotLength, other.tributarySlotLength)
                 && Arrays.equals(tributarySlotBitmap, other.tributarySlotBitmap);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("tributaryPortNumber", tributaryPortNumber)
                .add("tributarySlotLength", tributarySlotLength)
                .add("tributarySlotBitmap", HexString.toHexString(tributarySlotBitmap))
                .toString();
    }

}

