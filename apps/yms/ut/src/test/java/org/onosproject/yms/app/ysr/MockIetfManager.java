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

package org.onosproject.yms.app.ysr;

import org.onosproject.event.ListenerRegistry;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network2.rev20151208.IetfNetwork2;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network2.rev20151208.IetfNetwork2OpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network2.rev20151208.IetfNetwork2Service;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network2.rev20151208.ietfnetwork2.IetfNetwork2Event;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network2.rev20151208.ietfnetwork2.IetfNetwork2EventListener;

/**
 * Represent mock implementation for services.
 */
public class MockIetfManager
        extends ListenerRegistry<IetfNetwork2Event, IetfNetwork2EventListener>
        implements IetfNetwork2Service {

    @Override
    public IetfNetwork2 getIetfNetwork2(IetfNetwork2OpParam ietfNetwork2) {
        return null;
    }

    @Override
    public void setIetfNetwork2(IetfNetwork2OpParam ietfNetwork2) {

    }

}
