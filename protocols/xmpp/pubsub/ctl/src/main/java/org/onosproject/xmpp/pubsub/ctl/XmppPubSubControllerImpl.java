/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.pubsub.ctl;

import org.dom4j.Element;
import org.onosproject.net.DeviceId;
import org.onosproject.xmpp.core.XmppController;
import org.onosproject.xmpp.core.XmppDeviceId;
import org.onosproject.xmpp.core.XmppIqListener;
import org.onosproject.xmpp.pubsub.XmppPubSubConstants;
import org.onosproject.xmpp.pubsub.XmppPubSubController;
import org.onosproject.xmpp.pubsub.XmppPublishEventsListener;
import org.onosproject.xmpp.pubsub.XmppSubscribeEventsListener;
import org.onosproject.xmpp.pubsub.model.XmppEventNotification;
import org.onosproject.xmpp.pubsub.model.XmppPubSubError;
import org.onosproject.xmpp.pubsub.model.XmppPublish;
import org.onosproject.xmpp.pubsub.model.XmppRetract;
import org.onosproject.xmpp.pubsub.model.XmppSubscribe;
import org.onosproject.xmpp.pubsub.model.XmppUnsubscribe;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.xmpp.pubsub.XmppPubSubConstants.PUBSUB_ELEMENT;
import static org.onosproject.xmpp.pubsub.XmppPubSubConstants.PUBSUB_NAMESPACE;

/**
 * The main class implementing XMPP Publish/Subscribe extension.
 * It listens to IQ stanzas and generates PubSub events based on the payload.
 */
@Component(immediate = true, service = XmppPubSubController.class)
public class XmppPubSubControllerImpl implements XmppPubSubController {

    private static final Logger log =
            LoggerFactory.getLogger(XmppPubSubControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected XmppController xmppController;

    protected Set<XmppPublishEventsListener> xmppPublishEventsListeners =
            new CopyOnWriteArraySet<XmppPublishEventsListener>();
    protected Set<XmppSubscribeEventsListener> xmppSubscribeEventsListeners =
            new CopyOnWriteArraySet<XmppSubscribeEventsListener>();

    protected XmppIqListener iqListener = new InternalXmppIqListener();

    @Activate
    public void activate() {
        xmppController.addXmppIqListener(iqListener, PUBSUB_NAMESPACE);
        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        xmppController.removeXmppIqListener(iqListener, PUBSUB_NAMESPACE);
        log.info("Stopped");
    }

    @Override
    public void notify(DeviceId deviceId, XmppEventNotification eventNotification) {
        XmppDeviceId xmppDeviceId = asXmppDeviceId(deviceId);
        xmppController.getDevice(xmppDeviceId).sendPacket(eventNotification);
    }

    @Override
    public void notifyError(DeviceId deviceId, XmppPubSubError error) {
        XmppDeviceId xmppDeviceId = asXmppDeviceId(deviceId);
        xmppController.getDevice(xmppDeviceId).sendError(error.asPacketError());
    }

    private XmppDeviceId asXmppDeviceId(DeviceId deviceId) {
        String[] parts = deviceId.toString().split(":");
        JID jid = new JID(parts[1]);
        return new XmppDeviceId(jid);
    }

    @Override
    public void addXmppPublishEventsListener(XmppPublishEventsListener xmppPublishEventsListener) {
        xmppPublishEventsListeners.add(xmppPublishEventsListener);
    }

    @Override
    public void removeXmppPublishEventsListener(XmppPublishEventsListener xmppPublishEventsListener) {
        xmppPublishEventsListeners.remove(xmppPublishEventsListener);
    }

    @Override
    public void addXmppSubscribeEventsListener(XmppSubscribeEventsListener xmppSubscribeEventsListener) {
        xmppSubscribeEventsListeners.add(xmppSubscribeEventsListener);
    }

    @Override
    public void removeXmppSubscribeEventsListener(XmppSubscribeEventsListener xmppSubscribeEventsListener) {
        xmppSubscribeEventsListeners.remove(xmppSubscribeEventsListener);
    }

    private class InternalXmppIqListener implements XmppIqListener {
        @Override
        public void handleIqStanza(IQ iq) {
            if (isPubSub(iq)) {
                notifyListeners(iq);
            }
        }
    }

    private void notifyListeners(IQ iq) {
        XmppPubSubConstants.Method method = getMethod(iq);
        checkNotNull(method);
        switch (method) {
            case SUBSCRIBE:
                XmppSubscribe subscribe = new XmppSubscribe(iq);
                notifyXmppSubscribe(subscribe);
                break;
            case UNSUBSCRIBE:
                XmppUnsubscribe unsubscribe = new XmppUnsubscribe(iq);
                notifyXmppUnsubscribe(unsubscribe);
                break;
            case PUBLISH:
                XmppPublish publish = new XmppPublish(iq);
                notifyXmppPublish(publish);
                break;
            case RETRACT:
                XmppRetract retract = new XmppRetract(iq);
                notifyXmppRetract(retract);
                break;
            default:
                break;
        }
    }

    private void notifyXmppRetract(XmppRetract retractEvent) {
        for (XmppPublishEventsListener listener : xmppPublishEventsListeners) {
            listener.handleRetract(retractEvent);
        }
    }

    private void notifyXmppPublish(XmppPublish publishEvent) {
        for (XmppPublishEventsListener listener : xmppPublishEventsListeners) {
            listener.handlePublish(publishEvent);
        }
    }

    private void notifyXmppUnsubscribe(XmppUnsubscribe unsubscribeEvent) {
        for (XmppSubscribeEventsListener listener : xmppSubscribeEventsListeners) {
            listener.handleUnsubscribe(unsubscribeEvent);
        }
    }

    private void notifyXmppSubscribe(XmppSubscribe subscribeEvent) {
        for (XmppSubscribeEventsListener listener : xmppSubscribeEventsListeners) {
            listener.handleSubscribe(subscribeEvent);
        }
    }

    private boolean isPubSub(IQ iq) {
        Element pubsub = iq.getElement().element(PUBSUB_ELEMENT);
        if (pubsub != null && pubsub.getNamespaceURI().equals(PUBSUB_NAMESPACE)) {
            return true;
        }
        return false;
    }

    public static XmppPubSubConstants.Method getMethod(IQ iq) {
        Element pubsubElement = iq.getChildElement();
        Element methodElement = getChildElement(pubsubElement);
        String name = methodElement.getName();
        switch (name) {
            case "subscribe":
                return XmppPubSubConstants.Method.SUBSCRIBE;
            case "unsubscribe":
                return XmppPubSubConstants.Method.UNSUBSCRIBE;
            case "publish":
                return XmppPubSubConstants.Method.PUBLISH;
            case "retract":
                return XmppPubSubConstants.Method.RETRACT;
            default:
                break;
        }
        return null;
    }

    public static Element getChildElement(Element element) {
        Element child = (Element) element.elements().get(0); // the first element is related to pubsub operation
        return child;
    }

}
