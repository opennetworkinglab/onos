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

package org.onosproject.yangutils.datamodel;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/*
 * Reference RFC 6020.
 *
 * The bits built-in type represents a bit set.  That is, a bits value
 * is a set of flags identified by small integer position numbers
 * starting at 0.  Each bit number has an assigned name.
 */

/**
 * Represents the bits data type information.
 */
public class YangBits implements Parsable, Serializable {

    private static final long serialVersionUID = 806201641L;
    private static final String SPACE = " ";

    // Bits name
    private String bitsName;
    // Bits data contains bit-positions will be used to send to ONOS application
    private BitSet bitDataSet;
    /**
     * Mapping bit name to YangBit. In data input (jason), only bit name will be received.
     * By using the bit name corresponding (yang) bit-position will be retrieved from bitNameMap map.
     */
    private Map<String, YangBit> bitNameMap;
    /**
     * Mapping bit position to YangBit. The bit-position received from ONOS application
     * will be converted into bit-name by using bitPositionMap map to send (jason) output data as response.
     */
    private Map<Integer, YangBit> bitPositionMap;

    /**
     * Creates a YANG bits type object.
     */
    public YangBits() {
        bitDataSet = new BitSet();
        setBitNameMap(new HashMap<>());
        setBitPositionMap(new HashMap<>());
    }

    /**
     * Creates an instance of YANG bits.
     *
     * @param bits set of bit names
     * @throws DataModelException due to violation in data model rules
     */
    public YangBits(String bits) throws DataModelException {
        String[] bitNames = bits.trim().split(Pattern.quote(SPACE));
        setBitDataSet(bitNames);
    }

    /**
     * Returns the bits name.
     *
     * @return the bits name
     */
    public String getBitsName() {
        return bitsName;
    }

    /**
     * Sets the bits name.
     *
     * @param bitsName the bits name
     */
    public void setBitsName(String bitsName) {
        this.bitsName = bitsName;
    }

    /**
     * Returns the bit data set.
     *
     * @return the bit data set
     */
    public BitSet getBitDataSet() {
        return bitDataSet;
    }

    /**
     * Sets the bit data set.
     *
     * @param bitNames the set of bit names
     * @throws DataModelException due to violation in data model rules
     */
    public void setBitDataSet(String[] bitNames) throws DataModelException {
        YangBit bit;
        for (String bitName : bitNames) {
            bit = bitNameMap.get(bitName);
            if (bit == null) {
                throw new DataModelException("YANG file error: Unable to find " +
                                                     "corresponding bit position for bit name: " + bitName);
            }
            bitDataSet.set(bit.getPosition());
        }
    }

    /**
     * Returns the bit name map.
     *
     * @return the bit name map
     */
    public Map<String, YangBit> getBitNameMap() {
        return bitNameMap;
    }

    /**
     * Sets the bit name map.
     *
     * @param bitNameMap the bit name map
     */
    public void setBitNameMap(Map<String, YangBit> bitNameMap) {
        this.bitNameMap = bitNameMap;
    }

    /**
     * Checks whether bit name already available.
     *
     * @param bitName bit name
     * @return true if bit name already available otherwise returns false
     */
    public boolean isBitNameExists(String bitName) {
        return bitNameMap.containsKey(bitName);
    }

    /**
     * Returns the bit position map.
     *
     * @return the bit position map
     */
    public Map<Integer, YangBit> getBitPositionMap() {
        return bitPositionMap;
    }

    /**
     * Sets the bit position map.
     *
     * @param bitPositionMap the bit position map
     */
    public void setBitPositionMap(Map<Integer, YangBit> bitPositionMap) {
        this.bitPositionMap = bitPositionMap;
    }

    /**
     * Checks whether bit position already available.
     *
     * @param bitPosition bit position
     * @return true if bit position already available otherwise returns false
     */
    public boolean isBitPositionExists(Integer bitPosition) {
        return bitPositionMap.containsKey(bitPosition);
    }

    /**
     * Adds bit info.
     *
     * @param bitInfo the bit information to be added
     * @throws DataModelException due to violation in data model rules
     */
    public void addBitInfo(YangBit bitInfo) throws DataModelException {
        if (bitNameMap.put(bitInfo.getBitName(), bitInfo) != null) {
            throw new DataModelException("YANG file error: Duplicate bit name detected, same as bit name \""
                                                 + bitInfo.getBitName() + "\"");
        }
        if (bitPositionMap.put(bitInfo.getPosition(), bitInfo) != null) {
            throw new DataModelException("YANG file error: Duplicate bit position detected, same as bit position \""
                    + bitInfo.getPosition() + "\"");
        }
    }

    /**
     * Returns the type of the data.
     *
     * @return ParsedDataType returns BITS_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.BITS_DATA;
    }

    @Override
    public String toString() {
        YangBit bit;
        String bits = new String();
        for (int i = bitDataSet.nextSetBit(0); i >= 0; i = bitDataSet.nextSetBit(i + 1)) {
            bit = bitPositionMap.get(i);
            if (bit == null) {
                return null;
            }
            if (bits.isEmpty()) {
                bits =  bit.getBitName();
            } else {
                bits += " " + bit.getBitName();
            }
        }
        return bits.trim();
    }

    /**
     * Returns the object of YANG bits based on specific set of bit names.
     *
     * @param bits set of bit names
     * @return Object of YANG bits
     */
    public static YangBits fromString(String bits) {
        try {
            return new YangBits(bits);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Validates the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validates the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }
}
