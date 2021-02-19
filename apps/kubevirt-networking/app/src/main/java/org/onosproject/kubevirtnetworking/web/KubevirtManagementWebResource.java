/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.web;

import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.lang.Thread.sleep;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.COMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.INIT;

/**
 * REST interface for synchronizing kubevirt network states and rules.
 */
@Path("management")
public class KubevirtManagementWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long SLEEP_MS = 5000; // we wait 5s for init each node
    private static final long TIMEOUT_MS = 10000; // we wait 10s

    /**
     * Synchronizes the flow rules.
     *
     * @return 200 OK with sync result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync/rules")
    public Response syncRules() {

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);

        service.completeNodes(WORKER).forEach(this::syncRulesBase);
        return ok(mapper().createObjectNode()).build();
    }

    private void syncRulesBase(KubevirtNode node) {
        KubevirtNode updated = node.updateState(INIT);
        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);
        service.updateNode(updated);

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        while (service.node(node.hostname()).state() != COMPLETE) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }

            if (service.node(node.hostname()).state() == COMPLETE) {
                break;
            } else {
                service.updateNode(updated);
                log.info("Failed to synchronize flow rules, retrying...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            log.info("Successfully synchronize flow rules for node {}!", node.hostname());
        } else {
            log.warn("Failed to synchronize flow rules for node {}.", node.hostname());
        }
    }
}
