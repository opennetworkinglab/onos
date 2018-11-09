/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;
import com.google.common.primitives.UnsignedLongs;
import org.onlab.util.Identifier;

/**
 * Representation of a label Id, a logical port identifier.
 */
@Beta
public final class OpticalLogicId extends Identifier<Long> {

    /**
     * Constructor, public creation is prohibited.
     */
    private OpticalLogicId(long id) {
        super(id);
    }

    /**
     * Returns the LabelId representing the specified long value.
     *
     * @param id identifier as long value
     * @return LabelId
     */
    public static OpticalLogicId logicId(long id) {
        return new OpticalLogicId(id);
    }

    /**
     * Returns the LabelId representing the specified string value.
     *
     * @param string identifier as string value
     * @return LabelId
     */
    public static OpticalLogicId logicId(String string) {
        return new OpticalLogicId(UnsignedLongs.decode(string));
    }

    public long toLong() {
        return identifier;
    }
}
