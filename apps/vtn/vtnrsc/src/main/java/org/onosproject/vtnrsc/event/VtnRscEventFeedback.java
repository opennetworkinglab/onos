/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc.event;

import java.util.Objects;

import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterInterface;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a VtnRsc event feedback.
 */
public class VtnRscEventFeedback {
    private final FloatingIp floaingtIp;
    private final Router router;
    private final RouterInterface routerInterface;

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param floatingIp the floating Ip
     */
    public VtnRscEventFeedback(FloatingIp floatingIp) {
        this.floaingtIp = checkNotNull(floatingIp, "floaintIp cannot be null");
        this.router = null;
        this.routerInterface = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param router the router
     */
    public VtnRscEventFeedback(Router router) {
        this.floaingtIp = null;
        this.router = checkNotNull(router, "router cannot be null");
        this.routerInterface = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param routerInterface the router interface
     */
    public VtnRscEventFeedback(RouterInterface routerInterface) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = checkNotNull(routerInterface,
                                            "routerInterface cannot be null");
    }

    /**
     * Returns floating IP.
     *
     * @return floaingtIp the floating IP
     */
    public FloatingIp floatingIp() {
        return floaingtIp;
    }

    /**
     * Returns router.
     *
     * @return router the router
     */
    public Router router() {
        return router;
    }

    /**
     * Returns router interface.
     *
     * @return routerInterface the router interface
     */
    public RouterInterface routerInterface() {
        return routerInterface;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floaingtIp, router, routerInterface);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VtnRscEventFeedback) {
            final VtnRscEventFeedback that = (VtnRscEventFeedback) obj;
            return Objects.equals(this.floaingtIp, that.floaingtIp)
                    && Objects.equals(this.router, that.router)
                    && Objects.equals(this.routerInterface, that.routerInterface);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("router", router)
                .add("floaingtIp", floaingtIp)
                .add("routerInterface", routerInterface)
                .toString();
    }
}
