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
package org.onosproject.store.hz;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.hazelcast.monitor.LocalQueueStats;

import org.onosproject.store.serializers.StoreSerializer;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wrapper around IQueue&lt;byte[]&gt; which serializes/deserializes
 * key and value using StoreSerializer.
 *
 * @param <T> type
 */
public class SQueue<T> implements IQueue<T> {

    private final IQueue<byte[]> q;
    private final StoreSerializer serializer;

    /**
     * Creates a SQueue instance.
     *
     * @param baseQueue base IQueue to use
     * @param serializer serializer to use for both key and value
     */
    public SQueue(IQueue<byte[]> baseQueue, StoreSerializer serializer) {
        this.q = checkNotNull(baseQueue);
        this.serializer = checkNotNull(serializer);
    }

    private byte[] serialize(Object key) {
        return serializer.encode(key);
    }

    private T deserialize(byte[] key) {
        return serializer.decode(key);
    }

    @Override
    public boolean add(T t) {
        return q.add(serialize(t));
    }

    @Override
    public boolean offer(T t) {
        return q.offer(serialize(t));
    }

    @Override
    public void put(T t) throws InterruptedException {
        q.put(serialize(t));
    }

    @Override
    public boolean offer(T t, long l, TimeUnit timeUnit) throws InterruptedException {
        return q.offer(serialize(t), l, timeUnit);
    }

    @Override
    public T take() throws InterruptedException {
        return deserialize(q.take());
    }

    @Override
    public T poll(long l, TimeUnit timeUnit) throws InterruptedException {
        return deserialize(q.poll(l, timeUnit));
    }

    @Override
    public int remainingCapacity() {
        return q.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return q.remove(serialize(o));
    }

    @Override
    public boolean contains(Object o) {
        return q.contains(serialize(o));
    }

    @Deprecated // not implemented yet
    @Override
    public int drainTo(Collection<? super T> collection) {
        throw new UnsupportedOperationException();
    }

    @Deprecated // not implemented yet
    @Override
    public int drainTo(Collection<? super T> collection, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove() {
        return deserialize(q.remove());
    }

    @Override
    public T poll() {
        return deserialize(q.poll());
    }

    @Override
    public T element() {
        return deserialize(q.element());
    }

    @Override
    public T peek() {
        return deserialize(q.peek());
    }

    @Override
    public int size() {
        return q.size();
    }

    @Override
    public boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return FluentIterable.from(q)
                .transform(new DeserializeVal())
                .iterator();
    }

    @Deprecated // not implemented yet
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Deprecated // not implemented yet
    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        throw new UnsupportedOperationException();
    }

    @Deprecated // not implemented yet
    @Override
    public boolean containsAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Deprecated // not implemented yet
    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException();
    }

    @Deprecated // not implemented yet
    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Deprecated // not implemented yet
    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public LocalQueueStats getLocalQueueStats() {
        return q.getLocalQueueStats();
    }


    @Override
    public String addItemListener(ItemListener<T> itemListener, boolean withValue) {
        ItemListener<byte[]> il = new ItemListener<byte[]>() {
            @Override
            public void itemAdded(ItemEvent<byte[]> item) {
                itemListener.itemAdded(new ItemEvent<T>(getName(item),
                                                        item.getEventType(),
                                                        deserialize(item.getItem()),
                                                        item.getMember()));
            }

            @Override
            public void itemRemoved(ItemEvent<byte[]> item) {
                itemListener.itemRemoved(new ItemEvent<T>(getName(item),
                                                          item.getEventType(),
                                                          deserialize(item.getItem()),
                                                          item.getMember()));
            }

            private String getName(ItemEvent<byte[]> item) {
                return (item.getSource() instanceof String) ?
                        (String) item.getSource() : item.getSource().toString();

            }
        };
        return q.addItemListener(il, withValue);
    }


    @Override
    public boolean removeItemListener(String registrationId) {
        return q.removeItemListener(registrationId);
    }

    @Deprecated
    @Override
    public Object getId() {
        return q.getId();
    }

    @Override
    public String getPartitionKey() {
        return q.getPartitionKey();
    }

    @Override
    public String getName() {
        return q.getName();
    }

    @Override
    public String getServiceName() {
        return q.getServiceName();
    }

    @Override
    public void destroy() {
        q.destroy();
    }

    private final class DeserializeVal implements Function<byte[], T> {
        @Override
        public T apply(byte[] input) {
            return deserialize(input);
        }
    }
}
