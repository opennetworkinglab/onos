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
package org.onosproject.cpman;

/**
 * Default control message implementation.
 */
public class DefaultControlMessage implements ControlMessage {

    private final Type type;
    private final long load;
    private final long rate;
    private final long count;
    private final long timeStamp;

    /**
     * Generates a control message instance using given type and statistic
     * information.
     *
     * @param type control message type
     * @param load control message load
     * @param rate control message rate
     * @param count control message count
     * @param timeStamp time stamp of the control message stats
     */
    public DefaultControlMessage(Type type, long load, long rate,
                                 long count, long timeStamp) {
        this.type = type;
        this.load = load;
        this.rate = rate;
        this.count = count;
        this.timeStamp = timeStamp;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public long load() {
        return load;
    }

    @Override
    public long rate() {
        return rate;
    }

    @Override
    public long count() {
        return count;
    }

    @Override
    public long timeStamp() {
        return timeStamp;
    }
}
