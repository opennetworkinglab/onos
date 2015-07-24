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
 *
 */

package org.onosproject.ui.topo;

import java.util.ArrayList;
import java.util.List;

/**
 * Models a panel displayed on the Topology View.
 */
public class PropertyPanel {

    private String title;
    private String typeId;
    private List<Prop> properties = new ArrayList<>();


    public PropertyPanel(String title, String typeId) {
        this.title = title;
        this.typeId = typeId;
    }

    public PropertyPanel add(Prop p) {
        properties.add(p);
        return this;
    }

    public String title() {
        return title;
    }

    public String typeId() {
        return typeId;
    }

    // TODO: consider protecting this?
    public List<Prop> properties() {
        return properties;
    }

    public PropertyPanel title(String title) {
        this.title = title;
        return this;
    }

    // TODO: add other builder-like setters here


    // ====================

    public static class Prop {
        public final String key;
        public final String value;

        public Prop(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String key() {
            return key;
        }

        public String value() {
            return value;
        }
    }

    // Auxiliary properties separator
    public static class Separator extends Prop {
        public Separator() {
            super("-", "");
        }
    }

}
