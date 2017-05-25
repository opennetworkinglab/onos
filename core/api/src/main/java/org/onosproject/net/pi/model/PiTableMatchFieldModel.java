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
 * Model of a table match field in a protocol-independent pipeline.
 */
@Beta
public interface PiTableMatchFieldModel {
    /**
     * Returns the match type of this key.
     *
     * @return a match type
     */
    PiMatchType matchType();

    /**
     * Returns the header field instance matched by this key.
     *
     * @return a header field value
     */
    PiHeaderFieldModel field();
}
