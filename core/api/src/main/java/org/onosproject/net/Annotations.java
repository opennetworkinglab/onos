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
package org.onosproject.net;

import java.util.Set;

/**
 * Represents an set of simple key/value string annotations.
 */
public interface Annotations {

    /**
     * Returns the set of keys for available annotations.
     *
     * @return annotation keys
     */
    Set<String> keys();

    /**
     * Returns the value of the specified annotation.
     *
     * @param key annotation key
     * @return annotation value
     */
    String value(String key);

}
