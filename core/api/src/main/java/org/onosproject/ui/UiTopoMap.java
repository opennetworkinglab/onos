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

/**
 * Represents user interface topology view overlay.
 */
public class UiTopoMap {

    private final String id;
    private final String description;
    private final String filePath;
    private final double scale;


    /**
     * Creates a new topology map.
     *
     * @param id map identifier
     * @param description map description
     * @param filePath map filePath,
     * @param scale map scale,
     */
    public UiTopoMap(String id, String description, String filePath, double scale) {
        this.id = id;
        this.description = description;
        this.filePath = filePath;
        this.scale = scale;
    }

    /**
     * Returns the identifier for this map.
     *
     * @return the identifier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the description for this map.
     *
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the filePath for this map.
     *
     * @return the filePath
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Returns the scale for this map.
     *
     * @return the scale
     */
    public double getScale() {
        return this.scale;
    }

}
