/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.codec;

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionSet;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

/**
 * Codec for ActionSet.
 */
public class ActionSetCodec extends
        AbstractCodec<PiActionSet, P4RuntimeOuterClass.ActionProfileActionSet, Object>  {

    @Override
    protected P4RuntimeOuterClass.ActionProfileActionSet encode(
            PiActionSet piActionSet, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final var actProActSetBuilder =
                P4RuntimeOuterClass.ActionProfileActionSet.newBuilder();
        for (PiActionSet.WeightedAction act : piActionSet.actions()) {
            // TODO: currently we don't set "watch_port" field
            final var actProfAct =
                    P4RuntimeOuterClass.ActionProfileAction.newBuilder();
            actProfAct.setAction(Codecs.CODECS.action().encode(
                    act.action(), null, pipeconf));
            actProfAct.setWeight(act.weight());
            actProActSetBuilder.addActionProfileActions(actProfAct.build());
        }
        return actProActSetBuilder.build();
    }

    @Override
    protected PiActionSet decode(
            P4RuntimeOuterClass.ActionProfileActionSet message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final var builder = PiActionSet.builder();
        for (P4RuntimeOuterClass.ActionProfileAction act : message.getActionProfileActionsList()) {
            final var piAction = Codecs.CODECS.action().decode(
                    act.getAction(), null, pipeconf);
            builder.addWeightedAction(piAction, act.getWeight());
        }
        return builder.build();
    }
}
