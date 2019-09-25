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

package org.onosproject.pipelines.fabric.impl.behaviour;

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of HandlerBehaviour for the fabric pipeconf
 * behaviors.
 * <p>
 * All sub-classes must implement a default constructor, used by the abstract
 * projectable model (i.e., {@link org.onosproject.net.Device#as(Class)}.
 */
public abstract class AbstractFabricHandlerBehavior extends AbstractHandlerBehaviour {

    protected final Logger log = getLogger(getClass());

    protected FabricCapabilities capabilities;

    /**
     * Creates a new instance of this behavior with the given capabilities.
     * Note: this constructor should be invoked only by other classes of this
     * package that can retrieve capabilities on their own.
     * <p>
     * When using the abstract projectable model (i.e., {@link
     * org.onosproject.net.Device#as(Class)}, capabilities will be set by the
     * driver manager when calling {@link #setHandler(DriverHandler)})
     *
     * @param capabilities capabilities
     */
    protected AbstractFabricHandlerBehavior(FabricCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Create a new instance of this behaviour. Used by the abstract projectable
     * model (i.e., {@link org.onosproject.net.Device#as(Class)}.
     */
    public AbstractFabricHandlerBehavior() {
        // Do nothing
    }

    @Override
    public void setHandler(DriverHandler handler) {
        super.setHandler(handler);
        final PiPipeconfService pipeconfService = handler().get(PiPipeconfService.class);
        setCapabilitiesFromHandler(handler().data().deviceId(), pipeconfService);
    }

    private void setCapabilitiesFromHandler(
            DeviceId deviceId, PiPipeconfService pipeconfService) {
        checkNotNull(deviceId);
        checkNotNull(pipeconfService);
        // Get pipeconf capabilities.
        final PiPipeconfId pipeconfId = pipeconfService.ofDevice(deviceId)
                .orElse(null);
        if (pipeconfId == null) {
            throw new IllegalStateException(format(
                    "Unable to get pipeconf ID of device %s", deviceId.toString()));
        }
        if (!pipeconfService.getPipeconf(pipeconfId).isPresent()) {
            throw new IllegalStateException(format(
                    "Pipeconf '%s' is not registered ", pipeconfId));
        }
        this.capabilities = new FabricCapabilities(
                pipeconfService.getPipeconf(pipeconfId).get());
    }
}
