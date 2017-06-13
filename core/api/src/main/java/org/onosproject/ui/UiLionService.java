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

package org.onosproject.ui;

import org.onlab.util.ItemNotFoundException;
import org.onosproject.ui.lion.LionBundle;

/**
 * Service for accessing registered localization bundles.
 */
public interface UiLionService {

    /**
     * Returns the bundle for the given key. If no such bundle is registered
     * an ItemNotFound exception will be thrown.
     *
     * @param key the bundle key
     * @return the associated bundle
     * @throws ItemNotFoundException if no bundle exists for
     *                               the given key
     */
    LionBundle getBundle(String key);

}
