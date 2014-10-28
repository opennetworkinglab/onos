/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.intent;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.intent.IntentOperation.Type.REPLACE;
import static org.onlab.onos.net.intent.IntentOperation.Type.SUBMIT;
import static org.onlab.onos.net.intent.IntentOperation.Type.WITHDRAW;

/**
 * Batch of intent submit/withdraw/replace operations.
 */
public final class IntentOperations {

    private final List<IntentOperation> operations;

    /**
     * Creates a batch of intent operations using the supplied list.
     *
     * @param operations list of intent operations
     */
    private IntentOperations(List<IntentOperation> operations) {
        this.operations = operations;
    }

    /**
     * List of operations that need to be executed as a unit.
     *
     * @return list of intent operations
     */
    public List<IntentOperation> operations() {
        return operations;
    }

    /**
     * Returns a builder for intent operation batches.
     *
     * @return intent operations builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for batches of intent operations.
     */
    public static final class Builder {

        private final ImmutableList.Builder<IntentOperation> builder = ImmutableList.builder();

        // Public construction is forbidden.
        private Builder() {
        }

        /**
         * Adds an intent submit operation.
         *
         * @param intent intent to be submitted
         * @return self
         */
        public Builder addSubmitOperation(Intent intent) {
            checkNotNull(intent, "Intent cannot be null");
            builder.add(new IntentOperation(SUBMIT, intent.id(), intent));
            return this;
        }

        /**
         * Adds an intent submit operation.
         *
         * @param oldIntentId intent to be replaced
         * @param newIntent   replacement intent
         * @return self
         */
        public Builder addReplaceOperation(IntentId oldIntentId, Intent newIntent) {
            checkNotNull(oldIntentId, "Intent ID cannot be null");
            checkNotNull(newIntent, "Intent cannot be null");
            builder.add(new IntentOperation(REPLACE, oldIntentId, newIntent));
            return this;
        }

        /**
         * Adds an intent submit operation.
         *
         * @param intentId identifier of the intent to be withdrawn
         * @return self
         */
        public Builder addWithdrawOperation(IntentId intentId) {
            checkNotNull(intentId, "Intent ID cannot be null");
            builder.add(new IntentOperation(WITHDRAW, intentId, null));
            return this;
        }

        /**
         * Builds a batch of intent operations.
         *
         * @return immutable batch of intent operations
         */
        public IntentOperations build() {
            return new IntentOperations(builder.build());
        }

    }
}
