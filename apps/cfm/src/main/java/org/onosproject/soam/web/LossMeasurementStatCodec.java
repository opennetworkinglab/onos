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
package org.onosproject.soam.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStat;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to LossMeasurementStat object.
 */
public class LossMeasurementStatCodec extends JsonCodec<LossMeasurementStat> {

    @Override
    public ObjectNode encode(LossMeasurementStat lmStat, CodecContext context) {
        checkNotNull(lmStat, "LM stat cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("elapsedTime", lmStat.elapsedTime().toString())
                .put("suspectStatus", String.valueOf(lmStat.suspectStatus()));

        if (lmStat.forwardTransmittedFrames() != null) {
            result = result.put("forwardTransmittedFrames",
                    lmStat.forwardTransmittedFrames().toString());
        }
        if (lmStat.forwardReceivedFrames() != null) {
            result = result.put("forwardReceivedFrames",
                    lmStat.forwardReceivedFrames().toString());
        }
        if (lmStat.forwardMinFrameLossRatio() != null) {
            result = result.put("forwardMinFrameLossRatio",
                    lmStat.forwardMinFrameLossRatio().toString());
        }
        if (lmStat.forwardMaxFrameLossRatio() != null) {
            result = result.put("forwardMaxFrameLossRatio",
                    lmStat.forwardMaxFrameLossRatio().toString());
        }
        if (lmStat.forwardAverageFrameLossRatio() != null) {
            result = result.put("forwardAverageFrameLossRatio",
                    lmStat.forwardAverageFrameLossRatio().toString());
        }
        if (lmStat.backwardTransmittedFrames() != null) {
            result = result.put("backwardTransmittedFrames",
                    lmStat.backwardTransmittedFrames().toString());
        }
        if (lmStat.backwardReceivedFrames() != null) {
            result = result.put("backwardReceivedFrames",
                    lmStat.backwardReceivedFrames().toString());
        }
        if (lmStat.backwardMinFrameLossRatio() != null) {
            result = result.put("backwardMinFrameLossRatio",
                    lmStat.backwardMinFrameLossRatio().toString());
        }
        if (lmStat.backwardMaxFrameLossRatio() != null) {
            result = result.put("backwardMaxFrameLossRatio",
                    lmStat.backwardMaxFrameLossRatio().toString());
        }
        if (lmStat.backwardAverageFrameLossRatio() != null) {
            result = result.put("backwardAverageFrameLossRatio",
                    lmStat.backwardAverageFrameLossRatio().toString());
        }
        if (lmStat.soamPdusSent() != null) {
            result = result.put("soamPdusSent",
                    lmStat.soamPdusSent().toString());
        }
        if (lmStat.soamPdusReceived() != null) {
            result.put("soamPdusReceived",
                    lmStat.soamPdusReceived().toString());
        }

        return result;
    }
}
