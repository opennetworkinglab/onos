/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.config;


import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import org.onosproject.yang.model.ResourceId;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Abstraction for Filters that can be used while traversing the dynamic config store.
 * This abstraction allows to select entries of interest based on various criteria
 * defined by this interface.
 * NOTE: Only criteria based on {@code ResourceId} are supported currently.
 * This is a placeholder for a filter; Set of ResourceId becomes inefficient when
 * using a large number of filtering criteria;
 */
@Beta
public class Filter {
    /**
     * Traversal modes.
     */
    public enum TraversalMode {

        /**
         * SUB_TREE : if the node points to a subtree, the entire subtree will
         * be traversed; if pointing to a leaf, just the leaf will be retrieved.
         */
        SUB_TREE(-1),
        /**
         * NODE_ONLY : tree will not be traversed; will retrieve just the
         * specific node, irrespective of it being a subtree root or a leaf node.
         */
        NODE_ONLY(0),
        /**
         * GIVEN_DEPTH : as many levels of the subtree as indicated by depth
         * field of filter that  will be traversed; if depth is greater than
         * the number of levels of children, the entire subtree will be
         * traversed and end the traversal, without throwing any errors.
         */
        GIVEN_DEPTH;

        int val;
        TraversalMode() {

        }
        TraversalMode(int val) {
            this.val = val;
        }
        int val() {
            return val;
        }
    }

    /**
     *  Filtering criteria.
     */
    private final Set<ResourceId> criteria;
    /**
     * Traversal mode; default is to read just the given node(NODE_ONLY).
     */
    private final TraversalMode mode;
    /**
     * depth of traversal; default value is 0.
     */
    private final int depth;

    /**
     * Creates a default Filter builder.
     *
     * @return Filter builder
     */
    public static FilterBuilder builder() {
        return new FilterBuilder();
    }

    /**
     * Creates a Filter builder based on {@code start}.
     *
     * @param start Filter to initialize builder with
     * @return Filter builder
     *
     */
    public static FilterBuilder builder(Filter start) {
        return new FilterBuilder(start);
    }

    /**
     * Creates a new Filter object.
     *
     * @param criteria  set of filtering criteria
     * @param mode traversal mode
     * @param depth depth of traversal
     */
    protected Filter(Set<ResourceId> criteria, TraversalMode mode, int depth) {
        this.criteria = ImmutableSet.copyOf(criteria);
        this.mode = mode;
        this.depth = depth;
    }

    /**
     * Returns the traversal mode.
     *
     *@return traversal mode
     */
    public TraversalMode mode() {
        return mode;
    }

    /**
     * Returns the depth.
     *
     *@return depth
     */
    public int depth() {
        return depth;
    }

    /**
     * Returns the criteria that are in place for a Filter.
     *
     * @return Set of ResourceId criteria
     */
    public Set<ResourceId> criteria() {
        return this.criteria;
    }

    /**
     * Method to create a filter that include all entries rejected by the criteria.
     *
     * @param original Filter object with a criteria set
     * @return Filter object with negated criteria set
     * @throws InvalidFilterException if the received Filter object
     * was null or if it had an empty criteria set
     */
    public static Filter negateFilter(Filter original) {
        throw new FailedException("Not yet implemented");
    }

    /**
     * Returns if the Filter has an empty criteria set.
     *
     * @return {@code true} if criteria set is empty, {@code false} otherwise.
     */
    public boolean isEmptyFilter() {
        return criteria.isEmpty();
    }

    public static final class FilterBuilder extends Builder<FilterBuilder> {

        private FilterBuilder(Filter start) {
            super(start.criteria, start.mode, start.depth);
        }

        private FilterBuilder() {
            super();
        }

        public Filter build() {
            return new Filter(criteria, mode, depth);
        }
    }

    public abstract static class Builder<B extends Builder<B>> {

        protected Set<ResourceId> criteria;
        protected TraversalMode mode;
        protected int depth;

        protected Builder() {
            this(ImmutableSet.of(), TraversalMode.NODE_ONLY, 0);
        }

        protected Builder(Set<ResourceId> criteria,
                          TraversalMode mode,
                          int depth) {
            this.criteria = new LinkedHashSet<>(criteria);
            this.mode = checkNotNull(mode);
            this.depth = depth;
        }

        /**
         * Adds a new ResourceId filtering criterion to a Filter object.
         * If the same ResourceId is already part of the criteria
         * for the object, it will not be added again, but will not throw any exceptions.
         * This will not check for the validity of the ResourceId.
         *
         * @param add new criterion
         * @return self
         */
        public B addCriteria(ResourceId add) {
            criteria.add(add);
            return (B) this;
        }

        /**
         * Adds new ResourceId filtering criteria to a Filter object.
         * If the same ResourceId is already part of the criteria
         * for the object, it will not be added again, but will not throw any exceptions.
         * This will not check for the validity of the ResourceId.
         *
         * @param addAll new criteria
         * @return self
         */
        public B addAllCriteria(Set<ResourceId> addAll) {
            criteria.addAll(addAll);
            return (B) this;
        }

        /**
         * Replaces ResourceId filtering criteria with the one specified.
         * This will not check for the validity of the ResourceId.
         *
         * @param criteria new criteria
         * @return self
         */
        public B setCriteria(Set<ResourceId> criteria) {
            this.criteria = (criteria);
            return (B) this;
        }

        /**
         * Sets the traversal mode.
         *
         * @param mode traversal mode
         * @return self
         */
        public B mode(TraversalMode mode) {
            this.mode = mode;
            return (B) this;
        }

        /**
         * Sets the depth.
         *
         * @param depth of traversal
         * @return self
         */
        public B depth(int depth) {
            this.depth = depth;
            return (B) this;
        }
        /**
         * Removes the given ResourceId filtering criterion from a Filter object.
         * If the ResourceId was NOT already part of the criteria for
         * the object, it will not be removed, but will not throw any exceptions.
         * This will not check for the validity of the ResourceId.
         *
         * @param remove criterion to be removed
         * @return self
         */
        public B removeCriteria(ResourceId remove) {
            criteria.remove(remove);
            return (B) this;
        }

        /**
         * Removes the given ResourceId filtering criteria from a Filter object.
         * If the ResourceId was NOT already part of the criteria for
         * the object, it will not be removed, but will not throw any exceptions.
         * This will not check for the validity of the ResourceId.
         *
         * @param removeAll criteria to be removed
         * @return self
         */
        public B removeAllCriteria(Set<ResourceId> removeAll) {
            criteria.removeAll(removeAll);
            return (B) this;
        }
    }
}