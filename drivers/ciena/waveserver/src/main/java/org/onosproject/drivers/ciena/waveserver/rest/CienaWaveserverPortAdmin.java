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

package org.onosproject.drivers.ciena.waveserver.rest;

import org.onlab.util.Tools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;


public class CienaWaveserverPortAdmin extends AbstractHandlerBehaviour
        implements PortAdmin {
    private CienaRestDevice restCiena;
    private final Logger log = getLogger(getClass());

    @Override
    public CompletableFuture<Boolean> disable(PortNumber number) {
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice, {}", e);
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(restCiena.disablePort(number));
    }

    @Override
    public CompletableFuture<Boolean> enable(PortNumber number) {
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice, {}", e);
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(restCiena.enablePort(number));
    }

    @Override
    public CompletableFuture<Boolean> isEnabled(PortNumber number) {
        return Tools.exceptionalFuture(
                new UnsupportedOperationException("isEnabled is not supported"));

    }
}
