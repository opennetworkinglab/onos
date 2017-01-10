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
package org.onosproject.config;


import org.onosproject.config.model.ResourceIdentifier;

import java.util.Set;

/**
 * Abstraction for Filters that can be used while traversing the dynamic config store.
 * This abstraction allows to select entries of interest based on various criteria
 * defined by this interface.
 * NOTE: Only criteria based on {@code ResourceIdentifier} are supported currently.
 * This is a placeholder for a filter; Set of ResourceIdentifier becomes inefficient when
 * using a large number of filtering criteria;
 */
public interface Filter {
    enum TraversalMode {
        /**
         * Traversal types.
         */
        SUB_TREE(-1),
        NODE_ONLY(0),
        GIVEN_DEPTH;

        /**
         * variable indicating the depth of traversal.
         * depth = -1 => if the node is pointing to a subtree, the entire subtree will be traversed;
         * if the node points to a leaf, just the leaf will be retrieved.
         * depth = 0 => tree will not be traversed; will retrieve just the specific node,
         * irrespective of it being a subtree root or a leaf node
         * depth = any other integer => that many levels of the subtree will be traversed;
         * if depth > the number of levels of children, the entire subtree will
         * be traversed and end the traversal, without throwing any errors.
         */
        int depth;

        TraversalMode() {

        }

        TraversalMode(int depth) {
            this.depth = depth;
        }

        int depth() {
            return depth;
        }
    }

    /**
     * Adds the traversal depth to the Filter object.
     * Various interpretations of depth are as mentioned.
     * Default traversal mode is to read just the given node(NODE_ONLY).
     *
     * @param depth new criteria
     */
    void addDepth(TraversalMode depth);

    /**
     * Adds new ResourceIdentifier filtering criteria to a Filter object.
     * If the same ResourceIdentifier is already part of the criteria
     * for the object, it will not be added again, but will not throw any exceptions.
     * This will not check for the validity of the ResourceIdentifier.
     *
     * @param add new criteria
     */
    void addCriteria(Set<ResourceIdentifier> add);

    /**
     * Removes the given ResourceIdentifier filtering criteria from a Filter object.
     * If the ResourceIdentifier was NOT already part of the criteria for
     * the object, it will not be removed, but will not throw any exceptions.
     * This will not check for the validity of the ResourceIdentifier.
     *
     * @param remove criteria to be removed
     */
    void removeCriteria(Set<ResourceIdentifier> remove);

    /**
     * Method to list all the ResourceIdentifier criteria that are in place for a Filter.
     *
     * @return Set of ResourceIdentifier criteria for this entity
     */
    Set<ResourceIdentifier> getCriteria();

    /**
     * Method to create a filter that include all entries rejected by the criteria.
     *
     * @param original filter object with a criteria set
     * @return Filter object with negated criteria set
     * @throws InvalidFilterException if the received Filter object
     * was null or if it had an empty criteria set
     */
    Filter negateFilter(Filter original);

    /**
     * Method to check if the Filter has an empty criteria set.
     *
     * @return {@code true} if criteria set is empty, {@code true} otherwise.
     */
    boolean isEmptyFilter();
}