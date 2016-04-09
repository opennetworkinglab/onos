/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject;

import org.onosproject.core.ApplicationId;

import java.util.Objects;

/**
 * Test application ID.
 */
public class TestApplicationId implements ApplicationId {

    private final String name;
    private final short id;

    public TestApplicationId(String name) {
        this.name = name;
        this.id = (short) Objects.hash(name);
    }

    public static ApplicationId create(String name) {
        return new TestApplicationId(name);
    }

    @Override
    public short id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }
}
