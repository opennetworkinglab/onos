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
package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

/**
 * Representation of a label.
 */
@Beta
public final class LabelResourceId extends Identifier<Long> {

    /**
     * Creates a new label identifier.
     *
     * @param labelResourceId backing identifier value
     * @return label identifier
     */
    public static LabelResourceId labelResourceId(long labelResourceId) {
        return new LabelResourceId(labelResourceId);
    }

    // Public construction is prohibited
    private LabelResourceId(long labelId) {
        super(labelId);
    }

    /**
     * Returns label identifier.
     *
     * @return label identifier
     */
    public long labelId() {
        return identifier;
    }
}
