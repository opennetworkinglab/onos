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
package org.onosproject.cpman.impl.message;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.message.ControlMessageEvent;
import org.onosproject.cpman.message.ControlMessageStore;
import org.onosproject.cpman.message.ControlMessageStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of control message using trivial in-memory structures
 * implementation.
 */
@Component(immediate = true)
@Service
public class DefaultControlMessageStore
        extends AbstractStore<ControlMessageEvent, ControlMessageStoreDelegate>
        implements ControlMessageStore {

    private final Logger log = getLogger(getClass());

    @Override
    public ControlMessageEvent updateStatsInfo(ProviderId providerId, DeviceId deviceId,
                                                     Set<ControlMessage> controlMessages) {

        return new ControlMessageEvent(ControlMessageEvent.Type.STATS_UPDATE, controlMessages);
    }

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }
}
