/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.stratum;

import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Abstract implementation of a driver behaviour for Stratum devices that
 * provides access to protocol-specific implementations of the same behavior.
 *
 * @param <B> type of behaviour
 */
public abstract class AbstractStratumBehaviour<B extends HandlerBehaviour>
        extends AbstractHandlerBehaviour {

    protected B p4runtime;
    protected B gnmi;
    protected B gnoi;

    public AbstractStratumBehaviour(B p4runtime, B gnmi, B gnoi) {
        this.p4runtime = p4runtime;
        this.gnmi = gnmi;
        this.gnoi = gnoi;
    }

    @Override
    public void setHandler(DriverHandler handler) {
        super.setHandler(handler);
        p4runtime.setHandler(handler);
        gnmi.setHandler(handler);
        gnoi.setHandler(handler);
    }

    @Override
    public void setData(DriverData data) {
        super.setData(data);
        p4runtime.setData(data);
        gnmi.setData(data);
        gnoi.setData(data);
    }
}
