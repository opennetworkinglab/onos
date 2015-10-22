/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.net.flow.instructions;

import java.util.List;

/**
 * An extensible instruction type.
 */
public interface ExtensionInstruction {

    /**
     * Gets the type of the extension instruction.
     *
     * @return type
     */
    ExtensionType type();

    /**
     * Sets a property on the extension instruction.
     *
     * @param key property key
     * @param value value to set for the given key
     * @param <T> class of the value
     * @throws ExtensionPropertyException if the given key is not a valid
     * property on this extension instruction
     */
    <T> void setPropertyValue(String key, T value) throws ExtensionPropertyException;

    /**
     * Gets a property value of an extension instruction.
     *
     * @param key property key
     * @param <T> class of the value
     * @return value of the property
     * @throws ExtensionPropertyException if the given key is not a valid
     * property on this extension instruction
     */
    <T> T getPropertyValue(String key) throws ExtensionPropertyException;

    /**
     * Gets a list of all properties on the extension instruction.
     *
     * @return list of properties
     */
    List<String> getProperties();

    /**
     * Serialize the extension instruction to a byte array.
     *
     * @return byte array
     */
    byte[] serialize();

    /**
     * Deserialize the extension instruction from a byte array. The properties
     * of this object will be overwritten with the data in the byte array.
     *
     * @param data input byte array
     */
    void deserialize(byte[] data);


}
