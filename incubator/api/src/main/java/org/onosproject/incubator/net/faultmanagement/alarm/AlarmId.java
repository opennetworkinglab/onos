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
package org.onosproject.incubator.net.faultmanagement.alarm;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * Alarm identifier suitable as an external key.
 * <p>
 * This class is immutable.</p>
 */
@Beta
public final class AlarmId extends Identifier<Long> {

    public static final AlarmId NONE = new AlarmId();

    /**
     * Instantiates a new Alarm id.
     *
     * @param id the id
     */
    private AlarmId(long id) {
        super(id);
        checkArgument(id != 0L, "id must be non-zero");
    }

    private AlarmId() {
        super(0L);
    }

    /**
     * Creates an alarm identifier from the specified long representation.
     *
     * @param value long value
     * @return intent identifier
     */
    public static AlarmId alarmId(long value) {
        return new AlarmId(value);
    }

    /**
     * Returns the backing integer index.
     *
     * @return backing integer index
     */
    public long fingerprint() {
        return identifier;
    }
}
