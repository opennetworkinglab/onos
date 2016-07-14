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
package org.onosproject.ospf.controller;

/**
 * Enum representing OSPF neighbor state.
 */
public enum OspfNeighborState {

    DOWN(1),
    ATTEMPT(2),
    INIT(3),
    TWOWAY(4),
    EXSTART(5),
    EXCHANGE(6),
    LOADING(7),
    FULL(8);

    private int value;

    /**
     * Creates an OSPF neighbor state.
     *
     * @param value represents neighbors state
     */
    OspfNeighborState(int value) {
        this.value = value;
    }

    /**
     * Gets value of neighbor state.
     *
     * @return value represents neighbors state
     */
    public int getValue() {
        return value;
    }
}