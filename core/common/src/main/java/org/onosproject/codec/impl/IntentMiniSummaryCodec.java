/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.intent.util.IntentMiniSummary;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intent MiniSummary JSON codec.
 */
public class IntentMiniSummaryCodec extends JsonCodec<IntentMiniSummary> {
    private static final String TOTAL = "total";
    private static final String INSTALLREQ = "installReq";
    private static final String COMPILING = "compiling";
    private static final String INSTALLING = "installing";
    private static final String INSTALLED = "installed";
    private static final String RECOMPILING = "recompiling";
    private static final String WITHDRAWREQ = "withdrawReq";
    private static final String WITHDRAWING = "withdrawing";
    private static final String WITHDRAWN = "withdrawn";
    private static final String FAILED = "failed";
    private static final String UNKNOWNSTATE = "unknownState";

    @Override
    public ObjectNode encode(IntentMiniSummary intentminisummary, CodecContext context) {
        checkNotNull(intentminisummary, "intentminisummary cannot be null");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode()
                .put(TOTAL, intentminisummary.getTotal())
                .put(INSTALLED, intentminisummary.getInstalled())
                .put(FAILED, intentminisummary.getFailed())
                .put(INSTALLREQ, intentminisummary.getInstallReq())
                .put(INSTALLING, intentminisummary.getInstalling())
                .put(COMPILING, intentminisummary.getCompiling())
                .put(RECOMPILING, intentminisummary.getRecompiling())
                .put(WITHDRAWREQ, intentminisummary.getWithdrawReq())
                .put(WITHDRAWING, intentminisummary.getWithdrawing())
                .put(WITHDRAWN, intentminisummary.getWithdrawn())
                .put(UNKNOWNSTATE, intentminisummary.getUnknownState());
        return result;
    }
}
