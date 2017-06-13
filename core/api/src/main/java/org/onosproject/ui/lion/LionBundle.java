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
 *
 */

package org.onosproject.ui.lion;

/**
 * Encapsulates a bundle of localization strings.
 */
public final class LionBundle {

    private final String key;

    private LionBundle(String key) {
        this.key = key;
    }

    /**
     * Returns the key identifying this bundle.
     *
     * @return the bundle's key
     */
    public String key() {
        return key;
    }


    // TODO: public static method to generate a bundle from a ResourceBundle
}
