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

package org.onosproject.yms.app.ynh;

import org.onosproject.event.Event;
import org.onosproject.event.EventListener;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.event.ListenerService;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.app.ytb.DefaultYangTreeBuilder;
import org.onosproject.yms.app.ytb.YangTreeBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ynh.YangNotification;
import org.onosproject.yms.ynh.YangNotificationEvent;
import org.onosproject.yms.ynh.YangNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Representation of YANG notification manager.
 */
public class YangNotificationManager
        extends ListenerRegistry<YangNotificationEvent, YangNotificationListener>
        implements YangNotificationExtendedService {

    private static final String YANG_NOTIFICATION = "yangnotification";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ExecutorService executor;

    /**
     * YANG notification abstract listener. This listener will listens
     * abstractly to all the notification from the application to which it
     * has subscribed.
     */
    private YnhAbstractListener listener;

    /**
     * Maintains schema registry.
     */
    private YangSchemaRegistry schemaRegistry;

    /**
     * Creates an instance of YANG notification manager.
     *
     * @param registry YANG schema registry
     */
    public YangNotificationManager(YangSchemaRegistry registry) {
        listener = new YnhAbstractListener();
        executor = Executors.newSingleThreadExecutor(groupedThreads(
                "onos/yms", "event-handler-%d", log));
        schemaRegistry = registry;
    }

    @Override
    public void registerAsListener(ListenerService manager) {
        manager.addListener(listener);
    }

    @Override
    public YangNotification getFilteredSubject(YangNotification subject,
                                               YangNotification filter) {
        return null;
        // TODO
    }

    /**
     * Representation of YANG notification handler's abstract listener. It
     * listens for events from application(s).
     */
    private class YnhAbstractListener<E extends Event> implements
            EventListener<E> {

        @Override
        public void event(Event event) {
            executor.execute(() -> {
                try {
                    log.info("Event received in ynh " + event.type());
                    /*
                     * Obtain YANG data tree corresponding to notification with
                     * logical root node as yangnotification, followed by
                     * module/sub-module, followed by notification.
                     */
                    YangTreeBuilder builder = new DefaultYangTreeBuilder();
                    YdtContext context = builder.getYdtForNotification(
                            event, YANG_NOTIFICATION, schemaRegistry);
                    /*
                     * Create YANG notification from obtained data tree and
                     * send it to registered protocols.
                     */
                    YangNotification notification =
                            new YangNotification(context);
                    process(new YangNotificationEvent(notification));
                } catch (Exception e) {
                    log.warn("Failed to process {}", event, e);
                }
            });
        }
    }
}
