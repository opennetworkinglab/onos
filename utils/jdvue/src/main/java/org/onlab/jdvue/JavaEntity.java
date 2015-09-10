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
package org.onlab.jdvue;

import java.util.Objects;

/**
 * Abstraction of a Java source entity.
 */
public abstract class JavaEntity {

    private final String name;

    /**
     * Creates a new Java source entity with the given name.
     *
     * @param name source entity name
     */
    JavaEntity(String name) {
        this.name = name;
    }

    /**
     * Returns the Java source entity name.
     *
     * @return source entity name
     */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JavaEntity) {
            JavaEntity that = (JavaEntity) o;
            return getClass().equals(that.getClass()) &&
                    Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
