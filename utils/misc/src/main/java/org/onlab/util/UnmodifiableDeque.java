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
package org.onlab.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.google.common.collect.Iterators;

/**
 * Unmodifiable view of the specified Deque.
 */
public class UnmodifiableDeque<E> implements Deque<E> {

    private final Deque<E> deque;

    UnmodifiableDeque(Deque<E> deque) {
        this.deque = checkNotNull(deque);
    }

    /**
     * Returns an unmodifiable view of the specified Deque.
     *
     * @param <T> type
     * @param deque underlying {@link Deque} to use.
     * @return unmodifiable view of {@code deque}
     */
    public static <T> Deque<T> unmodifiableDeque(Deque<T> deque) {
        return new UnmodifiableDeque<T>(deque);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        deque.forEach(action);
    }

    @Override
    public void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    @Override
    public void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        return deque.toArray();
    }

    @Override
    public boolean offerFirst(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return deque.toArray(a);
    }

    @Override
    public boolean offerLast(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E removeFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E removeLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E getFirst() {
        return deque.getFirst();
    }

    @Override
    public E getLast() {
        return deque.getLast();
    }

    @Override
    public E peekFirst() {
        return deque.peekFirst();
    }

    @Override
    public E peekLast() {
        return deque.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return deque.containsAll(c);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E poll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E element() {
        return deque.element();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E peek() {
        return deque.peek();
    }

    @Override
    public void push(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return deque.equals(o);
    }

    @Override
    public boolean contains(Object o) {
        return deque.contains(o);
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.unmodifiableIterator(deque.iterator());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return Iterators.unmodifiableIterator(deque.descendingIterator());
    }

    @Override
    public int hashCode() {
        return deque.hashCode();
    }

    @Override
    public Spliterator<E> spliterator() {
        return deque.spliterator();
    }

    @Override
    public Stream<E> stream() {
        return deque.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return deque.parallelStream();
    }

    @Override
    public String toString() {
        return deque.toString();
    }
}
