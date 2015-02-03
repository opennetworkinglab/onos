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
package org.onosproject.net.intent;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentOperation.Type.REPLACE;
import static org.onosproject.net.intent.IntentOperation.Type.SUBMIT;
import static org.onosproject.net.intent.IntentOperation.Type.UPDATE;
import static org.onosproject.net.intent.IntentOperation.Type.WITHDRAW;

/**
 * Batch of intent submit/withdraw/replace operations.
 */
@Deprecated //DELETEME
public final class IntentOperations {

    private final List<IntentOperation> operations;
    private final ApplicationId appId;

    /**
     * Creates a batch of intent operations using the supplied list.
     *
     * @param operations list of intent operations
     */
    private IntentOperations(List<IntentOperation> operations, ApplicationId appId) {
        checkNotNull(operations);
        checkNotNull(appId);
        // TODO: consider check whether operations are not empty because empty batch is meaningless
        // but it affects the existing code to add this checking

        this.operations = operations;
        this.appId = appId;
    }

    /**
     * List of operations that need to be executed as a unit.
     *
     * @return list of intent operations
     */
    public List<IntentOperation> operations() {
        return operations;
    }

    public ApplicationId appId() {
        return appId;
    }

    /**
     * Returns a builder for intent operation batches.
     *
     * @return intent operations builder
     * @param applicationId application id
     */
    public static Builder builder(ApplicationId applicationId) {
        return new Builder(applicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final IntentOperations other = (IntentOperations) obj;
        return Objects.equals(this.operations, other.operations);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("operations", operations)
                .toString();
    }

    /**
     * Builder for batches of intent operations.
     */
    public static final class Builder {

        private final ImmutableList.Builder<IntentOperation> builder = ImmutableList.builder();
        private final ApplicationId appId;

        // Public construction is forbidden.
        private Builder(ApplicationId appId) {
            this.appId = appId;
        }

        /**
         * Adds an intent submit operation.
         *
         * @param intent intent to be submitted
         * @return self
         */
        public Builder addSubmitOperation(Intent intent) {
            checkNotNull(intent, "Intent cannot be null");
            builder.add(new IntentOperation(SUBMIT, intent));
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
            builder.add(new IntentOperation(REPLACE, newIntent)); //FIXME
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
            builder.add(new IntentOperation(WITHDRAW, null)); //FIXME
            return this;
        }

        /**
         * Adds an intent update operation.
         *
         * @param intentId identifier of the intent to be updated
         * @return self
         */
        public Builder addUpdateOperation(IntentId intentId) {
            checkNotNull(intentId, "Intent ID cannot be null");
            builder.add(new IntentOperation(UPDATE, null)); //FIXME
            return this;
        }

        /**
         * Builds a batch of intent operations.
         *
         * @return immutable batch of intent operations
         */
        public IntentOperations build() {
            return new IntentOperations(builder.build(), appId);
        }

    }
}
