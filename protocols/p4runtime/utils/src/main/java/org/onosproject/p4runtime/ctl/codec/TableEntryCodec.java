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

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiActionSet;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableEntryHandle;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;

import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for P4Runtime TableEntry.
 */
public final class TableEntryCodec
        extends AbstractEntityCodec<PiTableEntry, PiTableEntryHandle,
        P4RuntimeOuterClass.TableEntry, Object> {

    @Override
    protected P4RuntimeOuterClass.TableEntry encode(
            PiTableEntry piTableEntry, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final P4RuntimeOuterClass.TableEntry.Builder tableEntryMsgBuilder =
                keyMsgBuilder(piTableEntry.table(), piTableEntry.matchKey(),
                              piTableEntry.priority(), pipeconf, browser);
        // Controller metadata (cookie)
        tableEntryMsgBuilder.setControllerMetadata(piTableEntry.cookie());
        // Timeout.
        if (piTableEntry.timeout().isPresent()) {
            // FIXME: timeout is supported in P4Runtime v1.0
            log.warn("Found PI table entry with timeout set, " +
                             "not supported in P4Runtime: {}", piTableEntry);
        }
        // Table action.
        if (piTableEntry.action() != null) {
            tableEntryMsgBuilder.setAction(
                    encodePiTableAction(piTableEntry.action(), pipeconf));
        }
        // Counter.
        if (piTableEntry.counter() != null) {
            tableEntryMsgBuilder.setCounterData(encodeCounter(piTableEntry.counter()));
        }
        return tableEntryMsgBuilder.build();
    }

    @Override
    protected P4RuntimeOuterClass.TableEntry encodeKey(
            PiTableEntryHandle handle, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser) throws CodecException, P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(handle.tableId(), handle.matchKey(),
                             handle.priority(), pipeconf, browser).build();
    }

    @Override
    protected P4RuntimeOuterClass.TableEntry encodeKey(
            PiTableEntry piEntity, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser) throws CodecException, P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(piEntity.table(), piEntity.matchKey(),
                             piEntity.priority(), pipeconf, browser).build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private P4RuntimeOuterClass.TableEntry.Builder keyMsgBuilder(
            PiTableId tableId, PiMatchKey matchKey, OptionalInt priority,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, CodecException {
        final P4RuntimeOuterClass.TableEntry.Builder tableEntryMsgBuilder =
                P4RuntimeOuterClass.TableEntry.newBuilder();
        final P4InfoOuterClass.Preamble tablePreamble = browser.tables()
                .getByName(tableId.id()).getPreamble();
        // Table id.
        tableEntryMsgBuilder.setTableId(tablePreamble.getId());
        // Field matches.
        if (matchKey.equals(PiMatchKey.EMPTY)) {
            tableEntryMsgBuilder.setIsDefaultAction(true);
        } else {
            tableEntryMsgBuilder.addAllMatch(
                    CODECS.fieldMatch().encodeAll(
                            matchKey.fieldMatches(),
                            tablePreamble, pipeconf));
        }
        // Priority.
        priority.ifPresent(tableEntryMsgBuilder::setPriority);
        return tableEntryMsgBuilder;
    }

    @Override
    protected PiTableEntry decode(
            P4RuntimeOuterClass.TableEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        PiTableEntry.Builder piTableEntryBuilder = PiTableEntry.builder();

        P4InfoOuterClass.Preamble tablePreamble = browser.tables()
                .getById(message.getTableId()).getPreamble();

        // Table id.
        piTableEntryBuilder.forTable(PiTableId.of(tablePreamble.getName()));

        // Priority.
        if (message.getPriority() > 0) {
            piTableEntryBuilder.withPriority(message.getPriority());
        }

        // Controller metadata (cookie)
        piTableEntryBuilder.withCookie(message.getControllerMetadata());

        // Table action.
        if (message.hasAction()) {
            piTableEntryBuilder.withAction(decodeTableActionMsg(
                    message.getAction(), pipeconf));
        }

        // Timeout.
        // FIXME: how to decode table entry messages with timeout, given that
        //  the timeout value is lost after encoding?

        // Match key for field matches.
        piTableEntryBuilder.withMatchKey(
                PiMatchKey.builder()
                        .addFieldMatches(CODECS.fieldMatch().decodeAll(
                                message.getMatchList(),
                                tablePreamble, pipeconf))
                        .build());

        // Counter.
        if (message.hasCounterData()) {
            piTableEntryBuilder.withCounterCellData(decodeCounter(message.getCounterData()));
        }

        return piTableEntryBuilder.build();
    }

    private P4RuntimeOuterClass.TableAction encodePiTableAction(
            PiTableAction piTableAction, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(piTableAction, "Cannot encode null PiTableAction");
        final P4RuntimeOuterClass.TableAction.Builder tableActionMsgBuilder =
                P4RuntimeOuterClass.TableAction.newBuilder();
        switch (piTableAction.type()) {
            case ACTION:
                P4RuntimeOuterClass.Action theAction = CODECS.action()
                        .encode((PiAction) piTableAction, null, pipeconf);
                tableActionMsgBuilder.setAction(theAction);
                break;
            case ACTION_PROFILE_GROUP_ID:
                tableActionMsgBuilder.setActionProfileGroupId(
                        ((PiActionProfileGroupId) piTableAction).id());
                break;
            case ACTION_PROFILE_MEMBER_ID:
                tableActionMsgBuilder.setActionProfileMemberId(
                        ((PiActionProfileMemberId) piTableAction).id());
                break;
            case ACTION_SET:
                P4RuntimeOuterClass.ActionProfileActionSet theActionProfileActionSet =
                        CODECS.actionSet().encode(
                                (PiActionSet) piTableAction, null, pipeconf);
                tableActionMsgBuilder.setActionProfileActionSet(theActionProfileActionSet);
                break;
            default:
                throw new CodecException(
                        format("Building of table action type %s not implemented",
                               piTableAction.type()));
        }
        return tableActionMsgBuilder.build();
    }

    private PiTableAction decodeTableActionMsg(
            P4RuntimeOuterClass.TableAction tableActionMsg, PiPipeconf pipeconf)
            throws CodecException {
        P4RuntimeOuterClass.TableAction.TypeCase typeCase = tableActionMsg.getTypeCase();
        switch (typeCase) {
            case ACTION:
                P4RuntimeOuterClass.Action actionMsg = tableActionMsg.getAction();
                return CODECS.action().decode(
                        actionMsg, null, pipeconf);
            case ACTION_PROFILE_GROUP_ID:
                return PiActionProfileGroupId.of(
                        tableActionMsg.getActionProfileGroupId());
            case ACTION_PROFILE_MEMBER_ID:
                return PiActionProfileMemberId.of(
                        tableActionMsg.getActionProfileMemberId());
            case ACTION_PROFILE_ACTION_SET:
                return CODECS.actionSet().decode(
                        tableActionMsg.getActionProfileActionSet(), null, pipeconf);
            default:
                throw new CodecException(
                        format("Decoding of table action type %s not implemented",
                               typeCase.name()));
        }
    }

    private P4RuntimeOuterClass.CounterData encodeCounter(
            PiCounterCellData piCounterCellData) {
        return P4RuntimeOuterClass.CounterData.newBuilder()
                .setPacketCount(piCounterCellData.packets())
                .setByteCount(piCounterCellData.bytes()).build();
    }

    private PiCounterCellData decodeCounter(
            P4RuntimeOuterClass.CounterData counterData) {
        return new PiCounterCellData(
                counterData.getPacketCount(), counterData.getByteCount());
    }
}
