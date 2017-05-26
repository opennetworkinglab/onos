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

package org.onosproject.ui;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a geographically-based map to be used in the user interface
 * topology view. Instances of this class are immutable.
 */
public class UiTopoMap {

    private final String id;
    private final String desc;
    private final String filePath;
    private final double scale;
    private static final int MAX_LENGTH = 32;
    private static final String DES_EXC_LIM = "Description is too long";


    /**
     * Creates a new topology map.
     *
     * @param id       map identifier
     * @param desc     map description
     * @param filePath map filePath
     * @param scale    map scale
     */
    public UiTopoMap(String id, String desc, String filePath, double scale) {
        checkState(desc.length() <= MAX_LENGTH, DES_EXC_LIM);
        this.id = id;
        this.desc = desc;
        this.filePath = filePath;
        this.scale = scale;
    }

    /**
     * Returns the identifier for this map.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the description for this map.
     *
     * @return the description
     */
    public String description() {
        return desc;
    }

    /**
     * Returns the filePath for this map.
     *
     * @return the filePath
     */
    public String filePath() {
        return filePath;
    }

    /**
     * Returns the scale for this map.
     *
     * @return the scale
     */
    public double scale() {
        return scale;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("desc", desc)
                .toString();
    }
}
