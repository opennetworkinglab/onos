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

package org.onosproject.pce.pceservice;

/**
 * Representation of LSP type.
 */
public enum LspType {
    /**
     * Signifies that path is created via signaling mode.
     */
    WITH_SIGNALLING(0),

    /**
     * Signifies that path is created via SR mode.
     */
    SR_WITHOUT_SIGNALLING(1),

    /**
     * Signifies that path is created via without signaling and without SR mode.
     */
    WITHOUT_SIGNALLING_AND_WITHOUT_SR(2);

    int value;

    /**
     * Assign val with the value as the LSP type.
     *
     * @param val LSP type
     */
    LspType(int val) {
        value = val;
    }

    /**
     * Returns value of LSP type.
     *
     * @return LSP type
     */
    public byte type() {
        return (byte) value;
    }
}