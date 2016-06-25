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
package org.onosproject.ui;

import com.google.common.base.MoreObjects;

/**
 * Represents user interface view addition, except that this one should not
 * have an entry in the navigation panel.
 */
public class UiViewHidden extends UiView {

    /**
     * Creates a new user interface hidden view descriptor.
     *
     * @param id view identifier
     */
    public UiViewHidden(String id) {
        super(Category.HIDDEN, id, null);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .toString();
    }
}
