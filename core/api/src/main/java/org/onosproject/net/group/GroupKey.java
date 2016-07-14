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
package org.onosproject.net.group;

/**
 * Representation of generalized Key that would be used to store
 * groups in &lt; Key, Value &gt; store. This key uses a generic
 * byte array so that applications can associate their groups with
 * any of their data by translating it into a byte array.
 */
public interface GroupKey  {
    /**
     * Returns the byte representation of key.
     *
     * @return byte array
     */
    byte[] key();
}
