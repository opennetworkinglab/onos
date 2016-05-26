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
package org.onosproject.sfc.util;

import java.util.List;

import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;

public class MockExtensionSelector implements ExtensionSelector {

    private ExtensionSelectorType type;

    public MockExtensionSelector(ExtensionSelectorType type) {
        this.type = type;
    }

    @Override
    public <T> void setPropertyValue(String key, T value) throws ExtensionPropertyException {
    }

    @Override
    public <T> T getPropertyValue(String key) throws ExtensionPropertyException {
        return null;
    }

    @Override
    public List<String> getProperties() {
        return null;
    }

    @Override
    public byte[] serialize() {
        return null;
    }

    @Override
    public void deserialize(byte[] data) {
    }

    @Override
    public ExtensionSelectorType type() {
        return type;
    }
}