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
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossAvailabilityStat;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to LossAvailabilityStat object.
 */
public class LossAvailabilityStatCodec extends JsonCodec<LossAvailabilityStat> {

    @Override
    public ObjectNode encode(LossAvailabilityStat laStat, CodecContext context) {
        checkNotNull(laStat, "LA stat cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("elapsedTime", laStat.elapsedTime().toString())
                .put("suspectStatus", String.valueOf(laStat.suspectStatus()));

        if (laStat.forwardHighLoss() != null) {
            result = result.put("forwardHighLoss",
                    laStat.forwardHighLoss().toString());
        }
        if (laStat.backwardHighLoss() != null) {
            result = result.put("backwardHighLoss",
                    laStat.backwardHighLoss().toString());
        }
        if (laStat.forwardConsecutiveHighLoss() != null) {
            result = result.put("forwardConsecutiveHighLoss",
                    laStat.forwardConsecutiveHighLoss().toString());
        }
        if (laStat.backwardConsecutiveHighLoss() != null) {
            result = result.put("backwardConsecutiveHighLoss",
                    laStat.backwardConsecutiveHighLoss().toString());
        }
        if (laStat.forwardAvailable() != null) {
            result = result.put("forwardAvailable",
                    laStat.forwardAvailable().toString());
        }
        if (laStat.backwardAvailable() != null) {
            result = result.put("backwardAvailable",
                    laStat.backwardAvailable().toString());
        }
        if (laStat.forwardUnavailable() != null) {
            result = result.put("forwardUnavailable",
                    laStat.forwardUnavailable().toString());
        }
        if (laStat.backwardUnavailable() != null) {
            result = result.put("backwardUnavailable",
                    laStat.backwardUnavailable().toString());
        }
        if (laStat.backwardMinFrameLossRatio() != null) {
            result = result.put("backwardMinFrameLossRatio",
                    laStat.backwardMinFrameLossRatio().toString());
        }
        if (laStat.backwardMaxFrameLossRatio() != null) {
            result = result.put("backwardMaxFrameLossRatio",
                    laStat.backwardMaxFrameLossRatio().toString());
        }
        if (laStat.backwardAverageFrameLossRatio() != null) {
            result = result.put("backwardAverageFrameLossRatio",
                    laStat.backwardAverageFrameLossRatio().toString());
        }
        if (laStat.forwardMinFrameLossRatio() != null) {
            result = result.put("forwardMinFrameLossRatio",
                    laStat.forwardMinFrameLossRatio().toString());
        }
        if (laStat.forwardMaxFrameLossRatio() != null) {
            result = result.put("forwardMaxFrameLossRatio",
                    laStat.forwardMaxFrameLossRatio().toString());
        }
        if (laStat.forwardAverageFrameLossRatio() != null) {
            result = result.put("forwardAverageFrameLossRatio",
                    laStat.forwardAverageFrameLossRatio().toString());
        }

        return result;
    }
}
