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
package org.onosproject.store.primitives.resources.impl;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.SerializerRegistry;
import io.atomix.copycat.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueueStats;

import com.google.common.base.MoreObjects;

/**
 * {@link AtomixWorkQueue} resource state machine operations.
 */
public final class AtomixWorkQueueCommands {

    private AtomixWorkQueueCommands() {
    }

    /**
     * Command to add a collection of tasks to the queue.
     */
    @SuppressWarnings("serial")
    public static class Add implements Command<Void>, CatalystSerializable {

        private Collection<byte[]> items;

        private Add() {
        }

        public Add(Collection<byte[]> items) {
            this.items = items;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeInt(items.size());
            items.forEach(task -> serializer.writeObject(task, buffer));
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            items = IntStream.range(0, buffer.readInt())
                             .mapToObj(i -> serializer.<byte[]>readObject(buffer))
                             .collect(Collectors.toCollection(ArrayList::new));
        }

        public Collection<byte[]> items() {
            return items;
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.QUORUM;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("items", items)
                    .toString();
        }
    }

    /**
     * Command to take a task from the queue.
     */
    @SuppressWarnings("serial")
    public static class Take implements Command<Collection<Task<byte[]>>>, CatalystSerializable {

        private int maxTasks;

        private Take() {
        }

        public Take(int maxTasks) {
            this.maxTasks = maxTasks;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeInt(maxTasks);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            maxTasks = buffer.readInt();
        }

        public int maxTasks() {
            return maxTasks;
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.QUORUM;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("maxTasks", maxTasks)
                    .toString();
        }
    }

    @SuppressWarnings("serial")
    public static class Stats implements Command<WorkQueueStats>, CatalystSerializable {

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }



    @SuppressWarnings("serial")
    public static class Register implements Command<Void>, CatalystSerializable {

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.QUORUM;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }

    @SuppressWarnings("serial")
    public static class Unregister implements Command<Void>, CatalystSerializable {

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }

    @SuppressWarnings("serial")
    public static class Complete implements Command<Void>, CatalystSerializable {
        private Collection<String> taskIds;

        private Complete() {
        }

        public Complete(Collection<String> taskIds) {
            this.taskIds = taskIds;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            serializer.writeObject(taskIds, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            taskIds = serializer.readObject(buffer);
        }

        public Collection<String> taskIds() {
            return taskIds;
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("taskIds", taskIds)
                    .toString();
        }
    }

    @SuppressWarnings("serial")
    public static class Clear implements Command<Void>, CatalystSerializable {

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }

    /**
     * Work queue command type resolver.
     */
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            registry.register(Register.class, -960);
            registry.register(Unregister.class, -961);
            registry.register(Take.class, -962);
            registry.register(Add.class, -963);
            registry.register(Complete.class, -964);
            registry.register(Stats.class, -965);
            registry.register(Clear.class, -966);
        }
    }
}
