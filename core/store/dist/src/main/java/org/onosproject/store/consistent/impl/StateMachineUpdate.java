/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of a state machine update.
 */
public class StateMachineUpdate {

    /**
     * Target data structure type this update is for.
     */
    enum Target {
        /**
         * Update is for a map.
         */
        MAP_UPDATE,

        /**
         * Update is a transaction commit.
         */
        TX_COMMIT,

        /**
         * Update is a queue push.
         */
        QUEUE_PUSH,

        /**
         * Update is for some other operation.
         */
        OTHER
    }

    private final String operationName;
    private final Object input;
    private final Object output;

    public StateMachineUpdate(String operationName, Object input, Object output) {
        this.operationName = operationName;
        this.input = input;
        this.output = output;
    }

    public Target target() {
        // FIXME: This check is brittle
        if (operationName.contains("mapUpdate")) {
            return Target.MAP_UPDATE;
        } else if (operationName.contains("commit") || operationName.contains("prepareAndCommit")) {
            return Target.TX_COMMIT;
        } else if (operationName.contains("queuePush")) {
            return Target.QUEUE_PUSH;
        } else {
            return Target.OTHER;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T input() {
        return (T) input;
    }

    @SuppressWarnings("unchecked")
    public <T> T output() {
        return (T) output;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", operationName)
                .add("input", input)
                .add("output", output)
                .toString();
    }
}