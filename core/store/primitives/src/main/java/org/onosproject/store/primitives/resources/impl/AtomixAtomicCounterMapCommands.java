/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.store.primitives.resources.impl;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.SerializerRegistry;
import io.atomix.copycat.Command;
import io.atomix.copycat.Query;

/**
 * Atomic counter map commands.
 */
public final class AtomixAtomicCounterMapCommands {
    private AtomixAtomicCounterMapCommands() {
    }

    public abstract static class AtomicCounterMapCommand<V> implements Command<V>, CatalystSerializable {
        @Override
        public CompactionMode compaction() {
            return CompactionMode.SNAPSHOT;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    public abstract static class AtomicCounterMapQuery<V> implements Query<V>, CatalystSerializable {
        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    public abstract static class KeyCommand<V> extends AtomicCounterMapCommand<V> {
        private String key;

        public KeyCommand() {
        }

        public KeyCommand(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeString(key);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            key = buffer.readString();
        }
    }

    public abstract static class KeyQuery<V> extends AtomicCounterMapQuery<V> {
        private String key;

        public KeyQuery() {
        }

        public KeyQuery(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeString(key);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            key = buffer.readString();
        }
    }

    public static class KeyValueCommand<V> extends KeyCommand<V> {
        private long value;

        public KeyValueCommand() {
        }

        public KeyValueCommand(String key, long value) {
            super(key);
            this.value = value;
        }

        public long value() {
            return value;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            buffer.writeLong(value);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            value = buffer.readLong();
        }
    }

    public static class Get extends KeyQuery<Long> {
        public Get() {
        }

        public Get(String key) {
            super(key);
        }
    }

    public static class Put extends KeyValueCommand<Long> {
        public Put() {
        }

        public Put(String key, long value) {
            super(key, value);
        }
    }

    public static class PutIfAbsent extends KeyValueCommand<Long> {
        public PutIfAbsent() {
        }

        public PutIfAbsent(String key, long value) {
            super(key, value);
        }
    }

    public static class Replace extends KeyCommand<Boolean> {
        private long replace;
        private long value;

        public Replace() {
        }

        public Replace(String key, long replace, long value) {
            super(key);
            this.replace = replace;
            this.value = value;
        }

        public long replace() {
            return replace;
        }

        public long value() {
            return value;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            buffer.writeLong(replace);
            buffer.writeLong(value);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            replace = buffer.readLong();
            value = buffer.readLong();
        }
    }

    public static class Remove extends KeyCommand<Long> {
        public Remove() {
        }

        public Remove(String key) {
            super(key);
        }
    }

    public static class RemoveValue extends KeyValueCommand<Boolean> {
        public RemoveValue() {
        }

        public RemoveValue(String key, long value) {
            super(key, value);
        }
    }

    public static class IncrementAndGet extends KeyCommand<Long> {
        public IncrementAndGet() {
        }

        public IncrementAndGet(String key) {
            super(key);
        }
    }

    public static class DecrementAndGet extends KeyCommand<Long> {
        public DecrementAndGet(String key) {
            super(key);
        }

        public DecrementAndGet() {
        }
    }

    public static class GetAndIncrement extends KeyCommand<Long> {
        public GetAndIncrement() {
        }

        public GetAndIncrement(String key) {
            super(key);
        }
    }

    public static class GetAndDecrement extends KeyCommand<Long> {
        public GetAndDecrement() {
        }

        public GetAndDecrement(String key) {
            super(key);
        }
    }

    public abstract static class DeltaCommand extends KeyCommand<Long> {
        private long delta;

        public DeltaCommand() {
        }

        public DeltaCommand(String key, long delta) {
            super(key);
            this.delta = delta;
        }

        public long delta() {
            return delta;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            buffer.writeLong(delta);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            delta = buffer.readLong();
        }
    }

    public static class AddAndGet extends DeltaCommand {
        public AddAndGet() {
        }

        public AddAndGet(String key, long delta) {
            super(key, delta);
        }
    }

    public static class GetAndAdd extends DeltaCommand {
        public GetAndAdd() {
        }

        public GetAndAdd(String key, long delta) {
            super(key, delta);
        }
    }

    public static class Size extends AtomicCounterMapQuery<Integer> {
    }

    public static class IsEmpty extends AtomicCounterMapQuery<Boolean> {
    }

    public static class Clear extends AtomicCounterMapCommand<Void> {
    }

    /**
     * Counter map command type resolver.
     */
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            registry.register(Get.class, -790);
            registry.register(Put.class, -791);
            registry.register(PutIfAbsent.class, -792);
            registry.register(Replace.class, -793);
            registry.register(Remove.class, -794);
            registry.register(RemoveValue.class, -795);
            registry.register(IncrementAndGet.class, -796);
            registry.register(DecrementAndGet.class, -797);
            registry.register(GetAndIncrement.class, -798);
            registry.register(GetAndDecrement.class, -799);
            registry.register(AddAndGet.class, -800);
            registry.register(GetAndAdd.class, -801);
            registry.register(Size.class, -801);
            registry.register(IsEmpty.class, -801);
            registry.register(Clear.class, -801);
        }
    }
}
