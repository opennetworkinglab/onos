/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.Maps;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.lisp.ctl.LispController;
import org.onosproject.lisp.ctl.LispMessageListener;
import org.onosproject.lisp.ctl.LispRouter;
import org.onosproject.lisp.ctl.LispRouterAgent;
import org.onosproject.lisp.ctl.LispRouterFactory;
import org.onosproject.lisp.ctl.LispRouterId;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.lisp.msg.authentication.LispAuthenticationConfig;
import org.onosproject.lisp.msg.protocols.LispInfoReply;
import org.onosproject.lisp.msg.protocols.LispInfoRequest;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toConcurrentMap;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.getIntegerProperty;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.lisp.ctl.impl.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * LISP controller initiation class.
 */
@Component(immediate = true, service = LispController.class,
        property = {
                LISP_AUTH_KEY + "=" + LISP_AUTH_KEY_DEFAULT,
                LISP_AUTH_KEY_ID + ":Integer=" + LISP_AUTH_KEY_ID_DEFAULT,
                ENABLE_SMR + ":Boolean=" + ENABLE_SMR_DEFAULT,
        })
public class LispControllerImpl implements LispController {

    private static final String APP_ID = "org.onosproject.lisp-base";

    private static final Logger log = getLogger(LispControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** Authentication key which is used to calculate authentication data. */
    private String lispAuthKey = LISP_AUTH_KEY_DEFAULT;

    /** Authentication key id which denotes the authentication method used to calculate the authentication data. */
    private int lispAuthKeyId = LISP_AUTH_KEY_ID_DEFAULT;

    /** Enable to send SMR(Solicit Map Request) by map server; by default SMR is not activated. */
    private boolean enableSmr = false;

    ExecutorService executorMessages =
            newFixedThreadPool(4, groupedThreads("onos/lisp", "event-stats-%d", log));

    protected LispRouterAgent agent = new DefaultLispRouterAgent();

    ConcurrentMap<LispRouterId, LispRouter> connectedRouters = Maps.newConcurrentMap();

    final LispAuthenticationConfig authConfig = LispAuthenticationConfig.getInstance();
    LispControllerBootstrap bootstrap = new LispControllerBootstrap();

    private Set<LispRouterListener> lispRouterListeners = new CopyOnWriteArraySet<>();
    private Set<LispMessageListener> lispMessageListeners = new CopyOnWriteArraySet<>();

    private LispRouterFactory routerFactory = LispRouterFactory.getInstance();

    @Activate
    public void activate(ComponentContext context) {
        coreService.registerApplication(APP_ID, this::cleanup);
        cfgService.registerProperties(getClass());
        initAuthConfig(context.getProperties());
        routerFactory.setAgent(agent);
        bootstrap.start();
        log.info("Started");
    }

    /**
     * Shutdowns all listening channel and all LISP channels.
     * Clean information about routers before deactivating.
     */
    private void cleanup() {
        bootstrap.stop();
        routerFactory.cleanAgent();
        connectedRouters.values().forEach(LispRouter::disconnectRouter);
        connectedRouters.clear();
    }

    @Deactivate
    public void deactivate() {
        cleanup();
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        bootstrap.stop();
        bootstrap.start();
    }

    /**
     * Initializes authentication key and authentication method.
     *
     * @param properties a set of properties that contained in component context
     */
    private void initAuthConfig(Dictionary<?, ?> properties) {
        authConfig.updateLispAuthKey(get(properties, LISP_AUTH_KEY));
        authConfig.updateLispAuthKeyId(getIntegerProperty(properties, LISP_AUTH_KEY_ID));
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String lispAuthKeyStr = Tools.get(properties, LISP_AUTH_KEY);
        lispAuthKey = lispAuthKeyStr != null ? lispAuthKeyStr : LISP_AUTH_KEY_DEFAULT;
        authConfig.updateLispAuthKey(lispAuthKey);
        log.info("Configured. LISP authentication key is {}", lispAuthKey);

        Integer lispAuthMethodInt = Tools.getIntegerProperty(properties, LISP_AUTH_KEY_ID);
        if (lispAuthMethodInt == null) {
            lispAuthKeyId = LISP_AUTH_KEY_ID_DEFAULT;
            log.info("LISP authentication method is not configured, default value is {}", lispAuthKeyId);
        } else {
            lispAuthKeyId = lispAuthMethodInt;
            log.info("Configured. LISP authentication method is configured to {}", lispAuthKeyId);
        }
        authConfig.updateLispAuthKeyId(lispAuthKeyId);

        Boolean enableSmr = Tools.isPropertyEnabled(properties, ENABLE_SMR);
        if (enableSmr == null) {
            log.info("Enable SMR is not configured, " +
                             "using current value of {}", this.enableSmr);
        } else {
            this.enableSmr = enableSmr;
            log.info("Configured. Sending SMR through map server is {}",
                     this.enableSmr ? "enabled" : "disabled");
        }
    }

    @Override
    public Iterable<LispRouter> getRouters() {
        return connectedRouters.values();
    }

    @Override
    public Iterable<LispRouter> getSubscribedRouters() {
        return connectedRouters.entrySet()
                .stream()
                .filter(e -> e.getValue().isSubscribed())
                .collect(toConcurrentMap(Map.Entry::getKey,
                                         Map.Entry::getValue)).values();
    }

    @Override
    public LispRouter connectRouter(LispRouterId routerId) {
        if (connectedRouters.containsKey(routerId)) {
            log.debug("LISP router {} is already existing", routerId);
            return connectedRouters.get(routerId);
        } else {
            // TODO: currently we do not consider to add LISP router from netcfg
            log.warn("Adding router from netcfg is not supported currently");
            return null;
        }
    }

    @Override
    public void disconnectRouter(LispRouterId routerId, boolean remove) {
        if (!connectedRouters.containsKey(routerId)) {
            log.warn("LISP router {} is not existing", routerId);
        } else {
            connectedRouters.get(routerId).disconnectRouter();
            if (remove) {
                agent.removeConnectedRouter(routerId);
            }
        }
    }

    @Override
    public LispRouter getRouter(LispRouterId routerId) {
        return connectedRouters.get(routerId);
    }

    @Override
    public void addRouterListener(LispRouterListener listener) {
        if (!lispRouterListeners.contains(listener)) {
            lispRouterListeners.add(listener);
        }
    }

    @Override
    public void removeRouterListener(LispRouterListener listener) {
        lispRouterListeners.remove(listener);
    }

    @Override
    public void addMessageListener(LispMessageListener listener) {
        if (!lispMessageListeners.contains(listener)) {
            lispMessageListeners.add(listener);
        }
    }

    @Override
    public void removeMessageListener(LispMessageListener listener) {
        lispMessageListeners.remove(listener);
    }

    /**
     * Implementation of a LISP agent which is responsible for keeping track of
     * connected LISP routers and the state in which they are in.
     */
    public final class DefaultLispRouterAgent implements LispRouterAgent {

        private final Logger log = getLogger(DefaultLispRouterAgent.class);

        /**
         * Prevents object instantiation from external class.
         */
        private DefaultLispRouterAgent() {
        }

        @Override
        public boolean addConnectedRouter(LispRouterId routerId, LispRouter router) {

            if (connectedRouters.get(routerId) != null) {
                log.warn("Trying to add connectedRouter but found a previous " +
                                 "value for routerId: {}", routerId);
                return false;
            } else {
                log.info("Added router {}", routerId);
                connectedRouters.put(routerId, router);
                for (LispRouterListener listener : lispRouterListeners) {
                    listener.routerAdded(routerId);
                }
                return true;
            }
        }

        @Override
        public void removeConnectedRouter(LispRouterId routerId) {

            if (connectedRouters.get(routerId) == null) {
                log.error("Trying to remove router {} from connectedRouter " +
                                  "list but no element was found", routerId);
            } else {
                log.info("Removed router {}", routerId);
                connectedRouters.remove(routerId);
                for (LispRouterListener listener : lispRouterListeners) {
                    listener.routerRemoved(routerId);
                }
            }
        }

        @Override
        public void processUpstreamMessage(LispRouterId routerId, LispMessage message) {

            switch (message.getType()) {
                case LISP_MAP_REGISTER:
                case LISP_MAP_REQUEST:
                    executorMessages.execute(
                            new LispIncomingMessageHandler(routerId, message));
                    break;
                case LISP_INFO:
                    if (message instanceof LispInfoRequest) {
                        executorMessages.execute(
                                new LispIncomingMessageHandler(routerId, message));
                    } else {
                        log.warn("Not incoming LISP control message");
                    }
                    break;
                default:
                    log.warn("Not incoming LISP control message");
                    break;
            }
        }

        @Override
        public void processDownstreamMessage(LispRouterId routerId, LispMessage message) {

            switch (message.getType()) {
                case LISP_MAP_NOTIFY:
                case LISP_MAP_REPLY:
                    executorMessages.execute(
                            new LispOutgoingMessageHandler(routerId, message));
                    break;
                case LISP_INFO:
                    if (message instanceof LispInfoReply) {
                        executorMessages.execute(
                                new LispOutgoingMessageHandler(routerId, message));
                    } else {
                        log.warn("Not outgoing LISP control message");
                    }
                    break;
                default:
                    log.warn("Not outgoing LISP control message");
                    break;
            }
        }
    }

    /**
     * LISP message handler.
     */
    protected class LispMessageHandler implements Runnable {

        private final LispRouterId routerId;
        private final LispMessage message;
        private final boolean isIncoming;

        LispMessageHandler(LispRouterId routerId,
                           LispMessage message, boolean isIncoming) {
            this.routerId = routerId;
            this.message = message;
            this.isIncoming = isIncoming;
        }

        @Override
        public void run() {
            for (LispMessageListener listener : lispMessageListeners) {
                if (isIncoming) {
                    listener.handleIncomingMessage(routerId, message);
                } else {
                    listener.handleOutgoingMessage(routerId, message);
                }
            }
        }
    }

    /**
     * LISP incoming message handler.
     */
    protected final class LispIncomingMessageHandler
            extends LispMessageHandler implements Runnable {

        LispIncomingMessageHandler(LispRouterId routerId,
                                   LispMessage message) {
            super(routerId, message, true);
        }
    }

    /**
     * LISP outgoing message handler.
     */
    protected final class LispOutgoingMessageHandler
            extends LispMessageHandler implements Runnable {

        LispOutgoingMessageHandler(LispRouterId routerId,
                                   LispMessage message) {
            super(routerId, message, false);
        }
    }
}
