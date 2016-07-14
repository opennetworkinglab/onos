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
 * Represents an set of sparse key/value string annotations capable of carrying
 * annotation keys tagged for removal.
 */
public interface SparseAnnotations extends Annotations {

    /**
     * {@inheritDoc}
     * <p>
     * Note that this set includes keys for any attributes tagged for removal.
     * </p>
     */
    @Override
    Set<String> keys();

    /**
     * Indicates whether the specified key has been tagged as removed. This is
     * used for merging sparse annotation sets.
     *
     * @param key annotation key
     * @return true if the previous annotation has been tagged for removal
     */
    boolean isRemoved(String key);

}
