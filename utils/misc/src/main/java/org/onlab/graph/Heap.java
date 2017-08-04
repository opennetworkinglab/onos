/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.graph;

import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of an array-backed heap structure whose sense of order is
 * imposed by the provided comparator.
 * <p>
 * While this provides similar functionality to {@link java.util.PriorityQueue}
 * data structure, one key difference is that external entities can control
 * when to restore the heap property, which is done through invocation of the
 * {@link #heapify} method.
 * </p>
 * <p>
 * This class is not thread-safe and care must be taken to prevent concurrent
 * modifications.
 * </p>
 *
 * @param <T> type of the items on the heap
 */
public class Heap<T> {

    private final List<T> data;
    private final Comparator<T> comparator;

    /**
     * Creates a new heap backed by the specified list. In the interest of
     * efficiency, the list should be array-backed. Also, for the same reason,
     * the data is not copied and therefore, the caller must assure that the
     * backing data is not altered in any way.
     *
     * @param data       backing data list
     * @param comparator comparator for ordering the heap items
     */
    public Heap(List<T> data, Comparator<T> comparator) {
        this.data = checkNotNull(data, "Data cannot be null");
        this.comparator = checkNotNull(comparator, "Comparator cannot be null");
        heapify();
    }

    /**
     * Restores the heap property by re-arranging the elements in the backing
     * array as necessary following any heap modifications.
     */
    public void heapify() {
        for (int i = data.size() / 2; i >= 0; i--) {
            heapify(i);
        }
    }

    /**
     * Returns the current size of the heap.
     *
     * @return number of items in the heap
     */
    public int size() {
        return data.size();
    }

    /**
     * Returns true if there are no items in the heap.
     *
     * @return true if heap is empty
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Returns the most extreme item in the heap.
     *
     * @return heap extreme or null if the heap is empty
     */
    public T extreme() {
        return data.isEmpty() ? null : data.get(0);
    }

    /**
     * Extracts and returns the most extreme item from the heap.
     *
     * @return heap extreme or null if the heap is empty
     */
    public T extractExtreme() {
        if (!isEmpty()) {
            T extreme = extreme();

            data.set(0, data.get(data.size() - 1));
            data.remove(data.size() - 1);
            heapify();
            return extreme;
        }
        return null;
    }

    /**
     * Inserts the specified item into the heap and returns the modified heap.
     *
     * @param item item to be inserted
     * @return the heap self
     * @throws IllegalArgumentException if the heap is already full
     */
    public Heap<T> insert(T item) {
        data.add(item);
        bubbleUp();
        return this;
    }

    /**
     * Returns iterator to traverse the heap level-by-level. This iterator
     * does not permit removal of items.
     *
     * @return non-destructive heap iterator
     */
    public Iterator<T> iterator() {
        return ImmutableList.copyOf(data).iterator();
    }

    // Bubbles up the last item in the heap to its proper position to restore
    // the heap property.
    private void bubbleUp() {
        int child = data.size() - 1;
        while (child > 0) {
            int parent = child / 2;
            if (comparator.compare(data.get(child), data.get(parent)) < 0) {
                break;
            }
            swap(child, parent);
            child = parent;
        }
    }

    // Restores the heap property of the specified heap layer.
    private void heapify(int i) {
        int left = 2 * i + 1;
        int right = 2 * i;
        int extreme = i;

        if (left < data.size() &&
                comparator.compare(data.get(extreme), data.get(left)) < 0) {
            extreme = left;
        }

        if (right < data.size() &&
                comparator.compare(data.get(extreme), data.get(right)) < 0) {
            extreme = right;
        }

        if (extreme != i) {
            swap(i, extreme);
            heapify(extreme);
        }
    }

    // Swaps two heap items identified by their respective indexes.
    private void swap(int i, int k) {
        T aux = data.get(i);
        data.set(i, data.get(k));
        data.set(k, aux);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Heap) {
            Heap that = (Heap) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.comparator, that.comparator) &&
                    Objects.deepEquals(this.data, that.data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(comparator, data);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("data", data)
                .add("comparator", comparator)
                .toString();
    }

}
