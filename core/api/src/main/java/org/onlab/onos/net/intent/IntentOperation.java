/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.net.intent;

/**
 * Abstraction of an intent-related operation, e.g. add, remove, replace.
 */
public class IntentOperation {

    private final Type type;
    private final IntentId intentId;
    private final Intent intent;

    /**
     * Operation type.
     */
    enum Type {
        /**
         * Indicates that an intent should be added.
         */
        SUBMIT,

        /**
         * Indicates that an intent should be removed.
         */
        WITHDRAW,

        /**
         * Indicates that an intent should be replaced with another.
         */
        REPLACE
    }

    /**
     * Creates an intent operation.
     *
     * @param type operation type
     * @param intentId identifier of the intent subject to the operation
     * @param intent intent subject
     */
    IntentOperation(Type type, IntentId intentId, Intent intent) {
        this.type = type;
        this.intentId = intentId;
        this.intent = intent;
    }

    /**
     * Returns the type of the operation.
     *
     * @return operation type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the identifier of the intent to which this operation applies.
     *
     * @return intent identifier
     */
    public IntentId intentId() {
        return intentId;
    }

    /**
     * Returns the intent to which this operation applied. For remove,
     * this can be null.
     *
     * @return intent that is the subject of the operation; null for remove
     */
    public Intent intent() {
        return intent;
    }

}
