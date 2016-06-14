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

package org.onosproject.bmv2.demo.app.ecmp;

import com.google.common.collect.ImmutableMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2HeaderTypeModel;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.flow.criteria.ExtensionSelector;

import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.bmv2.demo.app.ecmp.EcmpFabricApp.ECMP_CONTEXT;
import static org.onosproject.bmv2.demo.app.ecmp.EcmpInterpreter.*;

/**
 * Builder of ECMP group table extension selector.
 */
public class EcmpGroupTableSelectorBuilder {

    private int groupId;
    private int selector;

    /**
     * Sets the ECMP group ID.
     *
     * @param groupId an integer value
     * @return this
     */
    public EcmpGroupTableSelectorBuilder withGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Sets the ECMP selector.
     *
     * @param selector an integer value
     * @return this
     */
    public EcmpGroupTableSelectorBuilder withSelector(int selector) {
        this.selector = selector;
        return this;
    }

    /**
     * Returns a new extension selector.
     *
     * @return an extension selector
     */
    public ExtensionSelector build() {
        Bmv2HeaderTypeModel headerTypeModel = ECMP_CONTEXT.configuration().headerType(ECMP_METADATA_T);
        int groupIdBitWidth = headerTypeModel.field(GROUP_ID).bitWidth();
        int selectorBitWidth = headerTypeModel.field(SELECTOR).bitWidth();

        try {
            ImmutableByteSequence groupIdBs = fitByteSequence(ImmutableByteSequence.copyFrom(groupId),
                                                              groupIdBitWidth);
            ImmutableByteSequence selectorBs = fitByteSequence(ImmutableByteSequence.copyFrom(selector),
                                                               selectorBitWidth);

            Bmv2ExactMatchParam groupIdMatch = new Bmv2ExactMatchParam(groupIdBs);
            Bmv2ExactMatchParam hashMatch = new Bmv2ExactMatchParam(selectorBs);

            return new Bmv2ExtensionSelector(ImmutableMap.of(
                    ECMP_METADATA + "." + GROUP_ID, groupIdMatch,
                    ECMP_METADATA + "." + SELECTOR, hashMatch));

        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new RuntimeException(e);
        }
    }
}
