/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.domain.DomainIntentOperation.Type.ADD;
import static org.onosproject.net.domain.DomainIntentOperation.Type.REMOVE;

/**
 * A batch of domain intent operations that are broken into stages.
 */
public class DomainIntentOperations {

    private final List<DomainIntentOperation> stages;
    private final DomainIntentOperationsContext callback;

    private DomainIntentOperations(List<DomainIntentOperation> stages,
                                   DomainIntentOperationsContext cb) {
        this.stages = stages;
        this.callback = cb;
    }

    // kryo-constructor
    protected DomainIntentOperations() {
        this.stages = null;
        this.callback = null;
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the domain intent operations as sets of stages that should be
     * executed sequentially.
     *
     * @return domain intent stages
     */
    public List<DomainIntentOperation> stages() {
        return stages;
    }

    /**
     * Returns the callback for this batch of operations.
     *
     * @return callback
     */
    public DomainIntentOperationsContext callback() {
        return callback;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("stages", stages)
                .toString();
    }

    /**
     * A builder for constructing domain intent operations.
     */
    public static final class Builder {

        private final ImmutableList.Builder<DomainIntentOperation> listBuilder =
                ImmutableList.builder();

        // prevent use of the default constructor outside of this file; use the above method
        private Builder() {
        }

        /**
         * Appends a domain intent add to the current stage.
         *
         * @param intent domain intent
         * @return this
         */
        public Builder add(DomainIntent intent) {
            listBuilder.add(new DomainIntentOperation(intent, ADD));
            return this;
        }

        /**
         * Appends an existing domain intent to the current stage.
         * @param domainIntentOperation domain intent operation
         * @return this
         */
        public Builder operation(DomainIntentOperation domainIntentOperation) {
            listBuilder.add(domainIntentOperation);
            return this;
        }

        /**
         * Appends a domain intent removal to the current stage.
         *
         * @param intent domain intent
         * @return this
         */
        // FIXME this is confusing, consider renaming
        public Builder remove(DomainIntent intent) {
            listBuilder.add(new DomainIntentOperation(intent, REMOVE));
            return this;
        }

        /**
         * Builds the immutable domain intent operations.
         *
         * @return domain intent operations
         */
        public DomainIntentOperations build() {
            return build(NullDomainIntentOperationsContext.getInstance());
        }

        /**
         * Builds the immutable domain intent operations.
         *
         * @param cb the callback to call when this operation completes
         * @return domain intent operations
         */
        public DomainIntentOperations build(DomainIntentOperationsContext cb) {
            checkNotNull(cb);
            return new DomainIntentOperations(listBuilder.build(), cb);
        }
    }
}
