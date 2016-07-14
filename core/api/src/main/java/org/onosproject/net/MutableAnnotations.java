/*
 * Copyright 2015-present Open Networking Laboratory
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

/**
 * Represents an mutable set of simple key/value string annotations.
 */
public interface MutableAnnotations extends Annotations {

    /**
     * Returns the value of the specified annotation.
     *
     * @param key   annotation key
     * @param value annotation value
     * @return self
     */
    MutableAnnotations set(String key, String value);

    /**
     * Clears the specified keys or the all keys if none were specified.
     *
     * @param keys keys to be cleared
     * @return self
     */
    MutableAnnotations clear(String... keys);

}
