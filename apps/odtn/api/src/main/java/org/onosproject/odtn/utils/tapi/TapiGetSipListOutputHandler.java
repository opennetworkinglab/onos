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

package org.onosproject.odtn.utils.tapi;

import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.getserviceinterfacepointlist.DefaultGetServiceInterfacePointListOutput;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.getserviceinterfacepointlist.getserviceinterfacepointlistoutput.DefaultSip;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.getserviceinterfacepointlist.getserviceinterfacepointlistoutput.Sip;

/**
 * Utility class to deal with TAPI RPC output with DCS.
 */
public final class TapiGetSipListOutputHandler
        extends TapiRpcOutputHandler<DefaultGetServiceInterfacePointListOutput> {

    private TapiGetSipListOutputHandler() {
        obj = new DefaultGetServiceInterfacePointListOutput();
    }

    public static TapiGetSipListOutputHandler create() {
        return new TapiGetSipListOutputHandler();
    }

    public TapiGetSipListOutputHandler addSip(Uuid sipId) {
        Sip sip = new DefaultSip();
        sip.uuid(sipId);
        obj.addToSip(sip);
        return this;
    }

}
