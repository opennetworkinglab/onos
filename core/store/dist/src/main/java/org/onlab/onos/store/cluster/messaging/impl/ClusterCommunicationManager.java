package org.onlab.onos.store.cluster.messaging.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.impl.CommunicationsDelegate;
import org.onlab.onos.store.cluster.impl.MessageSender;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.MessageSubscriber;

import java.util.Set;

/**
 * Implements the cluster communication services to use by other stores.
 */
@Component(immediate = true)
@Service
public class ClusterCommunicationManager
        implements ClusterCommunicationService, CommunicationsDelegate {

    // TODO: use something different that won't require synchronization
    private Multimap<MessageSubject, MessageSubscriber> subscribers = HashMultimap.create();
    private MessageSender messageSender;

    @Override
    public boolean send(ClusterMessage message, NodeId toNodeId) {
        return messageSender.send(toNodeId, message);
    }

    @Override
    public synchronized void addSubscriber(MessageSubject subject, MessageSubscriber subscriber) {
        subscribers.put(subject, subscriber);
    }

    @Override
    public synchronized void removeSubscriber(MessageSubject subject, MessageSubscriber subscriber) {
        subscribers.remove(subject, subscriber);
    }

    @Override
    public Set<MessageSubscriber> getSubscribers(MessageSubject subject) {
        return ImmutableSet.copyOf(subscribers.get(subject));
    }

    @Override
    public void dispatch(ClusterMessage message) {
        Set<MessageSubscriber> set = getSubscribers(message.subject());
        if (set != null) {
            for (MessageSubscriber subscriber : set) {
                subscriber.receive(message);
            }
        }
    }

    @Override
    public void setSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
}
