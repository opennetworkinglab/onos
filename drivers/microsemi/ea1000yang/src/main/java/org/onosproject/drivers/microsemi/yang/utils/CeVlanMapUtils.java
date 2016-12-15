/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi.yang.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;

/**
 * A set of static utilities that allow a ce-vlan-map to be decomposed.
 *
 * This is an implementation specific to Microsemi that encodes ce-vlan-map
 * in a similar way to Section 7.9 in the MEF 10.2 specification. That specification
 * suggests comma delimited lists of VIDs - this implementation adds on colons to
 * specify a contiguous range of IDs
 */
public final class CeVlanMapUtils {

    private CeVlanMapUtils() {
        //Do not allow this utility class to be instantiated
    }

    /**
     * Calculate the ceVlanMap in to a Set of values.
     *
     * From Yang description
     *   "This object indicates the CE-VLANs associated with the specific
     *   EVC on a UNI. CE-VLAN IDs have value of 0 to 4095. The CE-VLAN ID
     *   list can be a single value or multiple values separated by a delimiter.
     *   Some valid values are: '100', '1:10', '10,20,30', '1:4095'. In the
     *   first example only CE-VLAN ID 100 is associated with the VLAN map.
     *   In the second example the CE-VLAN map includes CE-VLAN IDs 1 through
     *   10 (range of values). The third example indicates three separate values
     *   that make up the CE-VLAN map. The last example indicates all CE-VLAN IDs
     *   are included in the map (range of values). ";
     *  reference
     *   "[MEF 6.1] 6.1; [MEF 7.2] 6.2.1.3";
     * @param ceVlanMap A list of vlan id's in the format described above
     * @return A set of vlan ids
     */
    public static Short[] getVlanSet(String ceVlanMap) {
        if (ceVlanMap == null || ceVlanMap.isEmpty()) {
            return new Short[0];
        }
        Set<Short> ceVlanSet = new TreeSet<Short>();

        String[] ceVlanMapCommas = ceVlanMap.split(",");
        for (String ceVlanMapComma:ceVlanMapCommas) {
            String[] ceVlanMapColon = ceVlanMapComma.split(":");
            if (ceVlanMapColon.length == 1) {
                ceVlanSet.add(Short.decode(ceVlanMapColon[0]));
            } else {
                short start = Short.decode(ceVlanMapColon[0]);
                short end = Short.decode(ceVlanMapColon[1]);
                if ((start < 0 || end > 4095)) {
                    return null;
                } else {
                    for (short i = start; i <= end; i++) {
                        ceVlanSet.add(i);
                    }
                }
            }
        }

        return ceVlanSet.toArray(new Short[ceVlanSet.size()]);
    }

    /**
     * Convert an array of vlan ids in to a string representation.
     * @param vlanArray An array of vlan ids
     * @return A string representation delimited by commas and colons
     */
    public static String vlanListAsString(Short[] vlanArray) {
        boolean colonPending = false;
        StringBuilder ceVlanMapBuilder = new StringBuilder();
        if (vlanArray.length == 0) {
            return "";
        } else if (vlanArray.length == 1 && vlanArray[0] == 0) {
            return "0";
        }

        //To ensure that there are no repeated or out-of-order elements we must convert to TreeSet
        TreeSet<Short> vlanSet = new TreeSet<>(Arrays.asList(vlanArray));

        if (vlanSet.first() == 0) {
            vlanSet.remove(vlanSet.first());
        }
        short prev = vlanSet.first();
        for (short s:vlanSet) {
            if (s == prev) {
                ceVlanMapBuilder.append(Short.valueOf(s));
                continue;
            } else if (prev == (s - 1)) {
                colonPending = true;
            } else {
                if (colonPending) {
                    ceVlanMapBuilder.append(":" + Short.valueOf(prev));
                    colonPending = false;
                }
                ceVlanMapBuilder.append("," + Short.valueOf(s));
            }
            prev = s;
        }
        if (colonPending) {
            ceVlanMapBuilder.append(":" + Short.valueOf(prev));
        }

        return ceVlanMapBuilder.toString();
    }

    /**
     * Add an additional vlan id to an existing string representation.
     * @param existingMap An array of vlan ids
     * @param newVlan The new vlan ID to add
     * @return A string representation delimited by commas and colons
     */
    public static String addtoCeVlanMap(String existingMap, Short newVlan) {
        Short[] vlanArray = getVlanSet(existingMap);
        TreeSet<Short> vlanSet = new TreeSet<>();
        for (Short vlan:vlanArray) {
            vlanSet.add(vlan);
        }

        vlanSet.add(newVlan);

        return vlanListAsString(vlanSet.toArray(new Short[vlanSet.size()]));
    }

    /**
     * If a string representation contains a '0' then remove it.
     *
     * Zero is an invalid VLAN id, and is used here as a place holder for null. Null can't
     * be used in the EA1000 device. Once any other vlan ids are added then the zero should
     * be removed. It is safe to call this method even if no zero is present - the method will
     * make no change in that case.
     *
     * @param existingMap An string representation of vlan ids, possibly containing a zero
     * @return A string representation delimited by commas and colons without zero
     */
    public static String removeZeroIfPossible(String existingMap) {
        if (existingMap == null || existingMap.isEmpty()) {
            return "0";
        } else if (existingMap == "0") {
            return existingMap;
        }
        return removeFromCeVlanMap(existingMap, (short) 0);
    }

    /**
     * Remove a vlan id from an existing string representation.
     * @param existingMap An array of vlan ids
     * @param vlanRemove The vlan ID to remove
     * @return A string representation delimited by commas and colons
     */
    public static String removeFromCeVlanMap(String existingMap, Short vlanRemove) {
        Short[] vlanArray = getVlanSet(existingMap);
        TreeSet<Short> vlanSet = new TreeSet<>();
        for (Short vlan:vlanArray) {
            if (vlan.shortValue() != vlanRemove.shortValue()) {
                vlanSet.add(vlan);
            }
        }

        return vlanListAsString(vlanSet.toArray(new Short[vlanSet.size()]));
    }

    /**
     * Combine vlan ids from two existing string representations.
     *
     * If there are overlapping elements and ranges, these are consolidated in to one.
     *
     * @param set1 A string containing a set of vlan ids
     * @param set2 A string containing a set of vlan ids
     * @return A string representation delimited by commas and colons
     */
    public static String combineVlanSets(String set1, String set2) {
        Short[] set1Array = getVlanSet(set1);
        Short[] set2Array = getVlanSet(set2);
        return vlanListAsString((Short[]) ArrayUtils.addAll(set1Array, set2Array));
    }
}
