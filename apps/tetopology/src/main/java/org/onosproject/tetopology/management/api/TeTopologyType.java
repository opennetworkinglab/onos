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

package org.onosproject.tetopology.management.api;

/**
 * TE Topology type enumeration.
 */
public enum TeTopologyType {

    /**
     * Native topology.
     */
    NATIVE(0),

    /**
     * Customized topology.
     */
    CUSTOMIZED(1),

    /**
     * Subordinate TE topology received from SB.
     */
    SUBORDINATE(2),

    /**
     * Configured TE topology received from NB.
     */
    CONFIGURED(3),

    /**
     * ANY - default value, used for topology filtering based on topology type.
     */
    ANY(4);

    private int teTopologyType;

    /**
     * Creates an instance of teTopologyType.
     *
     * @param value value of teTopologyType
     */
    TeTopologyType(int value) {
        teTopologyType = value;
    }

    /**
     * Returns the attribute teTopologyType.
     *
     * @return value of teTopologyType
     */
    public int teTopologyType() {
        return teTopologyType;
    }

    /**
     * Returns the object of teTopologyType from input String. Returns null
     * when string conversion fails or value is not recognized.
     *
     * @param valInString input String
     * @return Object of teTopologyType
     */
    public static TeTopologyType of(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Returns the object of teTopologyType from input integer. Returns null
     * when the integer value is not recognized.
     *
     * @param value value of teTopologyType
     * @return Object of corresponding TE topology type
     */
    public static TeTopologyType of(int value) {
        switch (value) {
            case 0:
                return TeTopologyType.NATIVE;
            case 1:
                return TeTopologyType.CUSTOMIZED;
            case 2:
                return TeTopologyType.SUBORDINATE;
            case 3:
                return TeTopologyType.CONFIGURED;
            case 4:
                return TeTopologyType.ANY;
            default :
                return null;
        }
    }
}
