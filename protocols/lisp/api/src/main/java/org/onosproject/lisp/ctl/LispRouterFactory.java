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
package org.onosproject.lisp.ctl;

import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * LISP router factory which returns concrete router object for the physical
 * LISP router in use.
 */
public final class LispRouterFactory {

    private final Logger log = getLogger(getClass());

    private LispRouterAgent agent;
    private Map<LispRouterId, LispRouter> routerMap = Maps.newConcurrentMap();

    // non-instantiable (except for our Singleton)
    private LispRouterFactory() {
    }

    /**
     * Configures LISP router agent only if it is not initialized.
     *
     * @param agent reference object of LISP router agent
     */
    public void setAgent(LispRouterAgent agent) {
        synchronized (agent) {
            if (this.agent == null) {
                this.agent = agent;
            } else {
                log.warn("LISP Router Agent has already been set.");
            }
        }
    }

    /**
     * Cleans up LISP router agent.
     */
    public void cleanAgent() {
        if (this.agent == null) {
            log.warn("LISP Router Agent is not configured.");
            return;
        }
        LispRouterAgent existingAgent = agent;
        synchronized (existingAgent) {
            this.agent = null;
        }
    }

    /**
     * Returns a LISP router instance.
     *
     * @param ipAddress IP address of LISP router
     * @return LISP router instance
     */
    public LispRouter getRouterInstance(IpAddress ipAddress) {
        LispRouterId routerId = new LispRouterId(ipAddress);
        if (!routerMap.containsKey(routerId)) {
            LispRouter router = new DefaultLispRouter(routerId);
            router.setAgent(agent);
            routerMap.put(routerId, router);
            return router;
        } else {
            return routerMap.get(routerId);
        }
    }

    /**
     * Returns all LISP routers.
     *
     * @return all LISP routers
     */
    public Collection<LispRouter> getRouters() {
        return routerMap.values();
    }

    /**
     * Returns an instance of LISP router agent factory.
     *
     * @return instance of LISP router agent factory
     */
    public static LispRouterFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";
        private static final LispRouterFactory INSTANCE = new LispRouterFactory();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}
