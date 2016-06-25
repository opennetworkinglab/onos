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

import java.util.Map;
import java.util.Set;

import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;


import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.SerializerRegistry;
import io.atomix.catalyst.util.Assert;
import io.atomix.copycat.Command;
import io.atomix.copycat.Query;

/**
 * {@link AtomixLeaderElector} resource state machine operations.
 */
public final class AtomixLeaderElectorCommands {

    private AtomixLeaderElectorCommands() {
    }

    /**
     * Abstract election query.
     */
    @SuppressWarnings("serial")
    public abstract static class ElectionQuery<V> implements Query<V>, CatalystSerializable {

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    /**
     * Abstract election topic query.
     */
    @SuppressWarnings("serial")
    public abstract static class TopicQuery<V> extends ElectionQuery<V> implements CatalystSerializable {
        String topic;

        public TopicQuery() {
        }

        public TopicQuery(String topic) {
          this.topic = Assert.notNull(topic, "topic");
        }

        /**
         * Returns the topic.
         * @return topic
         */
        public String topic() {
          return topic;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            serializer.writeObject(topic, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
          topic = serializer.readObject(buffer);
        }
    }

    /**
     * Abstract election command.
     */
    @SuppressWarnings("serial")
    public abstract static class ElectionCommand<V> implements Command<V>, CatalystSerializable {

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    /**
     * Listen command.
     */
    @SuppressWarnings("serial")
    public static class Listen extends ElectionCommand<Void> {
    }

    /**
     * Unlisten command.
     */
    @SuppressWarnings("serial")
    public static class Unlisten extends ElectionCommand<Void> {

        @Override
        public CompactionMode compaction() {
            return CompactionMode.QUORUM;
        }
    }

    /**
     * GetLeader query.
     */
    @SuppressWarnings("serial")
    public static class GetLeadership extends TopicQuery<Leadership> {

        public GetLeadership() {
        }

        public GetLeadership(String topic) {
            super(topic);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("topic", topic)
                    .toString();
        }
    }

    /**
     * GetAllLeaders query.
     */
    @SuppressWarnings("serial")
    public static class GetAllLeaderships extends ElectionQuery<Map<String, Leadership>> {
    }

    /**
     * GetElectedTopics query.
     */
    @SuppressWarnings("serial")
    public static class GetElectedTopics extends ElectionQuery<Set<String>> {
        private NodeId nodeId;

        public GetElectedTopics() {
        }

        public GetElectedTopics(NodeId nodeId) {
            this.nodeId = Assert.argNot(nodeId, nodeId == null, "nodeId cannot be null");
        }

        /**
         * Returns the nodeId to check.
         *
         * @return The nodeId to check.
         */
        public NodeId nodeId() {
            return nodeId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("nodeId", nodeId)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(nodeId, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            nodeId = serializer.readObject(buffer);
        }
    }

    /**
     * Enter and run for leadership.
     */
    @SuppressWarnings("serial")
    public static class Run extends ElectionCommand<Leadership> {
        private String topic;
        private NodeId nodeId;

        public Run() {
        }

        public Run(String topic, NodeId nodeId) {
            this.topic = Assert.argNot(topic, Strings.isNullOrEmpty(topic), "topic cannot be null or empty");
            this.nodeId = Assert.argNot(nodeId, nodeId == null, "nodeId cannot be null");
        }

        /**
         * Returns the topic.
         *
         * @return topic
         */
        public String topic() {
            return topic;
        }

        /**
         * Returns the nodeId.
         *
         * @return the nodeId
         */
        public NodeId nodeId() {
            return nodeId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("topic", topic)
                    .add("nodeId", nodeId)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeString(topic);
            buffer.writeString(nodeId.toString());
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            topic = buffer.readString();
            nodeId = new NodeId(buffer.readString());
        }
    }

    /**
     * Withdraw from a leadership contest.
     */
    @SuppressWarnings("serial")
    public static class Withdraw extends ElectionCommand<Void> {
        private String topic;

        public Withdraw() {
        }

        public Withdraw(String topic) {
            this.topic = Assert.argNot(topic, Strings.isNullOrEmpty(topic), "topic cannot be null or empty");
        }

        /**
         * Returns the topic.
         *
         * @return The topic
         */
        public String topic() {
            return topic;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("topic", topic)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeString(topic);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            topic = buffer.readString();
        }
    }

    /**
     * Command for administratively changing the leadership state for a node.
     */
    @SuppressWarnings("serial")
    public abstract static class ElectionChangeCommand<V> extends ElectionCommand<V>  {
        private String topic;
        private NodeId nodeId;

        ElectionChangeCommand() {
            topic = null;
            nodeId = null;
        }

        public ElectionChangeCommand(String topic, NodeId nodeId) {
            this.topic = topic;
            this.nodeId = nodeId;
        }

        /**
         * Returns the topic.
         *
         * @return The topic
         */
        public String topic() {
            return topic;
        }

        /**
         * Returns the nodeId to make leader.
         *
         * @return The nodeId
         */
        public NodeId nodeId() {
            return nodeId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("topic", topic)
                    .add("nodeId", nodeId)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeString(topic);
            buffer.writeString(nodeId.toString());
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            topic = buffer.readString();
            nodeId = new NodeId(buffer.readString());
        }
    }

    /**
     * Command for administratively anoint a node as leader.
     */
    @SuppressWarnings("serial")
    public static class Anoint extends ElectionChangeCommand<Boolean> {

        private Anoint() {
        }

        public Anoint(String topic, NodeId nodeId) {
            super(topic, nodeId);
        }
    }

    /**
     * Command for administratively promote a node as top candidate.
     */
    @SuppressWarnings("serial")
    public static class Promote extends ElectionChangeCommand<Boolean> {

        private Promote() {
        }

        public Promote(String topic, NodeId nodeId) {
            super(topic, nodeId);
        }
    }

    /**
     * Command for administratively evicting a node from all leadership topics.
     */
    @SuppressWarnings("serial")
    public static class Evict extends ElectionCommand<Void> {
        private NodeId nodeId;

        public Evict() {
        }

        public Evict(NodeId nodeId) {
            this.nodeId = nodeId;
        }

        /**
         * Returns the node identifier.
         *
         * @return The nodeId
         */
        public NodeId nodeId() {
            return nodeId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("nodeId", nodeId)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            buffer.writeString(nodeId.toString());
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            nodeId = new NodeId(buffer.readString());
        }
    }

    /**
     * Map command type resolver.
     */
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            registry.register(Run.class, -861);
            registry.register(Withdraw.class, -862);
            registry.register(Anoint.class, -863);
            registry.register(GetAllLeaderships.class, -864);
            registry.register(GetElectedTopics.class, -865);
            registry.register(GetLeadership.class, -866);
            registry.register(Listen.class, -867);
            registry.register(Unlisten.class, -868);
            registry.register(Promote.class, -869);
            registry.register(Evict.class, -870);
        }
    }
}
