/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.device.impl;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.config.ConfigOperator;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * Abstract base implementation for element operators.
 */
public abstract class BasicElementOperator implements ConfigOperator {

    /**
     * Sets all defined values from the element config on the supplied
     * annotations builder.
     *
     * @param cfg     element configuration
     * @param builder annotations builder
     */
    protected static void combineElementAnnotations(BasicElementConfig cfg,
                                                    DefaultAnnotations.Builder builder) {

        if (cfg.name() != null) {
            builder.set(AnnotationKeys.NAME, cfg.name());
        }
        if (cfg.uiType() != null) {
            builder.set(AnnotationKeys.UI_TYPE, cfg.uiType());
        }

        if (cfg.locType() != null) {
            builder.set(AnnotationKeys.LOC_TYPE, cfg.locType());
        }
        if (cfg.geoCoordsSet()) {
            builder.set(AnnotationKeys.LATITUDE, Double.toString(cfg.latitude()));
            builder.set(AnnotationKeys.LONGITUDE, Double.toString(cfg.longitude()));
        } else if (cfg.gridCoordsSet()) {
            builder.set(AnnotationKeys.GRID_Y, Double.toString(cfg.gridY()));
            builder.set(AnnotationKeys.GRID_X, Double.toString(cfg.gridX()));
        }

        if (cfg.rackAddress() != null) {
            builder.set(AnnotationKeys.RACK_ADDRESS, cfg.rackAddress());
        }
        if (cfg.owner() != null) {
            builder.set(AnnotationKeys.OWNER, cfg.owner());
        }
    }
}
