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

package org.onosproject.p4runtime.ctl.codec;

import com.google.protobuf.ByteString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.copyAndFit;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.p4runtime.ctl.codec.Utils.assertSize;

/**
 * Codec for P4Runtime Action.
 */
public final class ActionCodec
        extends AbstractCodec<PiAction, P4RuntimeOuterClass.Action, Object> {

    @Override
    protected P4RuntimeOuterClass.Action encode(
            PiAction piAction, Object ignored, PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final int actionId = browser.actions()
                .getByName(piAction.id().toString()).getPreamble().getId();
        final P4RuntimeOuterClass.Action.Builder actionMsgBuilder =
                P4RuntimeOuterClass.Action.newBuilder().setActionId(actionId);
        for (PiActionParam p : piAction.parameters()) {
            final P4InfoOuterClass.Action.Param paramInfo = browser.actionParams(actionId)
                    .getByName(p.id().toString());
            final ByteString paramValue;
            if (browser.isTypeString(paramInfo.getTypeName())) {
                paramValue = ByteString.copyFrom(p.value().asReadOnlyBuffer());
            } else {
                paramValue = ByteString.copyFrom(p.value().canonical().asReadOnlyBuffer());
                // Check size only if the param type is not a sdn_string
                assertSize(format("param '%s' of action '%s'", p.id(), piAction.id()),
                        paramValue, paramInfo.getBitwidth());
            }
            actionMsgBuilder.addParams(P4RuntimeOuterClass.Action.Param.newBuilder()
                                               .setParamId(paramInfo.getId())
                                               .setValue(paramValue)
                                               .build());
        }
        return actionMsgBuilder.build();
    }

    @Override
    protected PiAction decode(
            P4RuntimeOuterClass.Action message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, CodecException {
        final P4InfoBrowser.EntityBrowser<P4InfoOuterClass.Action.Param> paramInfo =
                browser.actionParams(message.getActionId());
        final String actionName = browser.actions()
                .getById(message.getActionId())
                .getPreamble().getName();
        final PiAction.Builder builder = PiAction.builder()
                .withId(PiActionId.of(actionName));
        for (P4RuntimeOuterClass.Action.Param p : message.getParamsList()) {
            final P4InfoOuterClass.Action.Param actionParam = paramInfo.getById(p.getParamId());
            final ImmutableByteSequence value;
            if (browser.isTypeString(actionParam.getTypeName())) {
                value = copyFrom(new String(p.getValue().toByteArray()));
            } else {
                try {
                    value = copyAndFit(p.getValue().asReadOnlyByteBuffer(),
                                       actionParam.getBitwidth());
                } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
                    throw new CodecException(e.getMessage());
                }
            }
            builder.withParameter(new PiActionParam(
                    PiActionParamId.of(actionParam.getName()), value));
        }
        return builder.build();
    }
}
