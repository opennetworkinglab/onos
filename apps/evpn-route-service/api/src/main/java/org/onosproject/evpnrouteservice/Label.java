/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnrouteservice;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents label of the route.
 */
public final class Label {
    private final int label;

    /**
     * Constructor to initialize parameters.
     *
     * @param label route label
     */
    private Label(int label) {
        this.label = label;
    }

    /**
     * Creates the label for evpn route.
     *
     * @param label label of evpn route
     * @return Label
     */
    public static Label label(int label) {
        return new Label(label);
    }

    /**
     * Returns the label.
     *
     * @return label
     */
    public int getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Label) {
            Label other = (Label) obj;
            return Objects.equals(label, other.label);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("label", label).toString();
    }
}
