/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onlab.warden;

import static com.google.common.base.Preconditions.checkState;

/**
 * Cell reservation record.
 */
final class Reservation {

    final String cellName;
    final String userName;
    final long time;
    final int duration;
    final String cellSpec;

    // Creates a new reservation record
    Reservation(String cellName, String userName, long time, int duration, String cellSpec) {
        this.cellName = cellName;
        this.userName = userName;
        this.time = time;
        this.duration = duration;
        this.cellSpec = cellSpec;
    }

    /**
     * Decodes reservation record from the specified line.
     *
     * @param line string line
     */
    Reservation(String line) {
        String[] fields = line.trim().split("\t");
        checkState(fields.length == 5, "Incorrect reservation encoding");
        this.cellName = fields[0];
        this.userName = fields[1];
        this.time = Long.parseLong(fields[2]);
        this.duration = Integer.parseInt(fields[3]);
        this.cellSpec = fields[4];
    }

    /**
     * Encodes reservation record into a string line.
     *
     * @return encoded string
     */
    String encode() {
        return String.format("%s\t%s\t%s\t%s\t%s\n", cellName, userName, time, duration, cellSpec);
    }

}
