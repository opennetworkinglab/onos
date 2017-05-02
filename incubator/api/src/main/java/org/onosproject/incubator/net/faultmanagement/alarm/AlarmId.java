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
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Alarm identifier suitable as an external key.
 * <p>
 * This class is immutable.</p>
 */
@Beta
public final class AlarmId extends Identifier<String> {

    /**
     * Instantiates a new Alarm id.
     *
     * @param id               the device id
     * @param uniqueIdentifier the unique identifier of the Alarm on that device
     */
    private AlarmId(DeviceId id, String uniqueIdentifier) {
        super(id.toString() + ":" + uniqueIdentifier);
        checkNotNull(id, "device id must not be null");
        checkNotNull(uniqueIdentifier, "unique identifier must not be null");
        checkArgument(!uniqueIdentifier.isEmpty(), "unique identifier must not be empty");
    }

    /**
     * Instantiates a new Alarm id, primarly meant for lookup.
     *
     * @param globallyUniqueIdentifier the globally unique identifier of the Alarm,
     *                                 device Id + local unique identifier on the device
     */
    private AlarmId(String globallyUniqueIdentifier) {
        super(globallyUniqueIdentifier);
        checkArgument(!globallyUniqueIdentifier.isEmpty(), "unique identifier must not be empty");
    }

    /**
     * Creates an alarm identifier from the specified device id and
     * unique identifier provided representation.
     *
     * @param id               device id
     * @param uniqueIdentifier per device unique identifier of the alarm
     * @return alarm identifier
     */
    public static AlarmId alarmId(DeviceId id, String uniqueIdentifier) {
        return new AlarmId(id, uniqueIdentifier);
    }

    /**
     * Creates an alarm identifier from the specified globally unique identifier.
     *
     * @param globallyUniqueIdentifier the globally unique identifier of the Alarm,
     *                                 device Id + local unique identifier on the device
     * @return alarm identifier
     */
    public static AlarmId alarmId(String globallyUniqueIdentifier) {
        return new AlarmId(globallyUniqueIdentifier);
    }

}
