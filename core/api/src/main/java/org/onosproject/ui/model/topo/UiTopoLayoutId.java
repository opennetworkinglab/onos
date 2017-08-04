/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.ui.model.topo;

import org.onlab.util.Identifier;

/**
 * Identifier of a topology layout.
 */
public final class UiTopoLayoutId extends Identifier<String> {

    private static final String DEFAULT_STR = "root";

    /**
     * Default topology layout identifier.
     */
    public static final UiTopoLayoutId DEFAULT_ID =
            UiTopoLayoutId.layoutId(DEFAULT_STR);

    // For serialization
    private UiTopoLayoutId() {
    }

    private UiTopoLayoutId(String value) {
        super(value);
    }

    /**
     * Returns the layout identifier created from the specified value.
     *
     * @param value string value
     * @return layout identifier
     */
    public static UiTopoLayoutId layoutId(String value) {
        return new UiTopoLayoutId(value);
    }

    /**
     * Returns true if this is the identifier for the default layout.
     *
     * @return true if this is the default layout identifier
     */
    public boolean isDefault() {
        return DEFAULT_STR.equals(identifier);
    }
}
