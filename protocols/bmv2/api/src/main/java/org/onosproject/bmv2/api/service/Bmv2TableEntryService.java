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

package org.onosproject.bmv2.api.service;


import com.google.common.annotations.Beta;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslator;
import org.onosproject.bmv2.api.runtime.Bmv2FlowRuleWrapper;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntryReference;
import org.onosproject.net.DeviceId;

/**
 * A service for managing BMv2 table entries.
 */
@Beta
public interface Bmv2TableEntryService {

    /**
     * Returns a flow rule translator.
     *
     * @return a flow rule translator
     */
    Bmv2FlowRuleTranslator getFlowRuleTranslator();

    /**
     * Binds the given ONOS flow rule with a BMv2 table entry reference.
     *
     * @param entryRef a table entry reference
     * @param rule     a BMv2 flow rule wrapper
     */
    void bind(Bmv2TableEntryReference entryRef, Bmv2FlowRuleWrapper rule);

    /**
     * Returns the ONOS flow rule associated with the given BMv2 table entry reference, or null if there's no such a
     * mapping.
     *
     * @param entryRef a table entry reference
     * @return a BMv2 flow rule wrapper
     */
    Bmv2FlowRuleWrapper lookup(Bmv2TableEntryReference entryRef);

    /**
     * Removes any flow rule previously bound with a given BMv2 table entry reference.
     *
     * @param entryRef a table entry reference
     */
    void unbind(Bmv2TableEntryReference entryRef);

    /**
     * Removes all bindings for a given device.
     *
     * @param deviceId a device ID
     */
    void unbindAll(DeviceId deviceId);
}
