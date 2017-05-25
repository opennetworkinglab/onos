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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

/**
 * Model of a header's field instance in a protocol-independent pipeline.
 */
@Beta
public interface PiHeaderFieldModel {
    /**
     * Returns the header instance of this field instance.
     *
     * @return a header instance
     */
    PiHeaderModel header();

    /**
     * Returns the type of this header's field instance.
     *
     * @return a field type value
     */
    PiHeaderFieldTypeModel type();
}
