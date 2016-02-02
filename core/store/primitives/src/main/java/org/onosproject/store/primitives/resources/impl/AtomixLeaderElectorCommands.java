/*
 * Copyright 2016 Open Networking Laboratory
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
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;
import io.atomix.copycat.client.Command;
import io.atomix.copycat.client.Query;

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
        public ConsistencyLevel consistency() {
            return ConsistencyLevel.BOUNDED_LINEARIZABLE;
        }

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
        public ConsistencyLevel consistency() {
            return ConsistencyLevel.LINEARIZABLE;
        }

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
    }

    /**
     * Command for administratively anointing a node as leader.
     */
    @SuppressWarnings("serial")
    public static class Anoint extends ElectionCommand<Boolean> {
        private String topic;
        private NodeId nodeId;

        public Anoint() {
        }

        public Anoint(String topic, NodeId nodeId) {
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
    }
}
