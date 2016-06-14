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

package org.onosproject.bmv2.demo.app.wcmp;

import com.google.common.collect.ImmutableMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.flow.criteria.ExtensionSelector;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.roundToBytes;
import static org.onosproject.bmv2.demo.app.wcmp.WcmpFabricApp.WCMP_CONTEXT;
import static org.onosproject.bmv2.demo.app.wcmp.WcmpInterpreter.*;

/**
 * Builder of WCMP group table extension selector.
 */
public final class WcmpGroupTableSelectorBuilder {

    private int groupId;
    private int prefixLength;

    /**
     * Sets the WCMP group ID.
     *
     * @param groupId an integer value
     * @return this
     */
    public WcmpGroupTableSelectorBuilder withGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Sets the WCMP selector's prefix length.
     *
     * @param prefixLength an integer value
     * @return this
     */
    public WcmpGroupTableSelectorBuilder withPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }

    /**
     * Returns a new extension selector.
     *
     * @return an extension selector
     */
    public ExtensionSelector build() {

        final int selectorBitWidth = WCMP_CONTEXT.configuration().headerType(WCMP_META_T).field(SELECTOR).bitWidth();
        final int groupIdBitWidth = WCMP_CONTEXT.configuration().headerType(WCMP_META_T).field(GROUP_ID).bitWidth();
        final ImmutableByteSequence ones = ImmutableByteSequence.ofOnes(roundToBytes(selectorBitWidth));

        checkArgument(prefixLength >= 1 && prefixLength <= selectorBitWidth,
                      "prefix length must be between 1 and " + selectorBitWidth);
        try {
            ImmutableByteSequence groupIdBs = fitByteSequence(ImmutableByteSequence.copyFrom(groupId), groupIdBitWidth);
            Bmv2ExactMatchParam groupIdMatch = new Bmv2ExactMatchParam(groupIdBs);
            Bmv2LpmMatchParam selectorMatch = new Bmv2LpmMatchParam(ones, prefixLength);

            return new Bmv2ExtensionSelector(ImmutableMap.of(
                    WCMP_META + "." + GROUP_ID, groupIdMatch,
                    WCMP_META + "." + SELECTOR, selectorMatch));

        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new RuntimeException(e);
        }
    }
}
