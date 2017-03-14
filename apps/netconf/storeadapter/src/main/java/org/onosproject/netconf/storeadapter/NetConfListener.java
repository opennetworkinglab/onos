/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.netconf.storeadapter;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;

import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true)
public class NetConfListener implements DynamicConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NetConfListener.class);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService cfgServcie;
    public static final String DEVNMSPACE = "namespace1";

    private ResourceId resId = new ResourceId.Builder()
            .addBranchPointSchema("device", DEVNMSPACE )
            .build();
    @Activate
    protected void activate() {
        cfgServcie.addListener(this);
        log.info("NetConfListener Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgServcie.removeListener(this);
        log.info("NetConfListener Stopped");
    }

    public boolean isRelevant(DynamicConfigEvent event) {
        if (event.subject().equals(resId)) {
            log.info("isRelevant {} = {}", resId, event.subject());
                return true;
            } else {
            log.info("isRelevant {} != {}", resId, event.subject());
            return false;
        }
    }

    public void event(DynamicConfigEvent event) {
        if (!isRelevant(event)) {
            log.info("event is not relevanyt!!!! {} != {}", resId, event.subject());
            return;
        }
        switch (event.type()) {
            case NODE_ADDED:
                log.info("NetConfListener: RXD NODE_ADDED event");
                Filter filt = new Filter();
                DataNode node = cfgServcie.readNode(event.subject(), filt);
                //call netconf passive
                break;
            case NODE_UPDATED:
                log.info("NetConfListener: RXD NODE_UPDATED event");
                break;
            case NODE_REPLACED:
                log.info("NetConfListener: RXD NODE_REPLACED event");
                break;
            case NODE_DELETED:
                log.info("NetConfListener: RXD NODE_DELETED event");
                break;
            case UNKNOWN_OPRN:
            default:
                log.warn("NetConfListener: unknown event: {}", event.type());
                break;
        }
    }
}