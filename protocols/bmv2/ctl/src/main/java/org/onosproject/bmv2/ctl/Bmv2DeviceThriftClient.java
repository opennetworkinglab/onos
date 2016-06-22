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

package org.onosproject.bmv2.ctl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransport;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2MatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ParsedTableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2PortInfo;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ValidMatchParam;
import org.onosproject.bmv2.thriftapi.BmActionEntry;
import org.onosproject.bmv2.thriftapi.BmAddEntryOptions;
import org.onosproject.bmv2.thriftapi.BmCounterValue;
import org.onosproject.bmv2.thriftapi.BmMatchParam;
import org.onosproject.bmv2.thriftapi.BmMatchParamExact;
import org.onosproject.bmv2.thriftapi.BmMatchParamLPM;
import org.onosproject.bmv2.thriftapi.BmMatchParamTernary;
import org.onosproject.bmv2.thriftapi.BmMatchParamType;
import org.onosproject.bmv2.thriftapi.BmMatchParamValid;
import org.onosproject.bmv2.thriftapi.BmMtEntry;
import org.onosproject.bmv2.thriftapi.SimpleSwitch;
import org.onosproject.bmv2.thriftapi.Standard;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.bmv2.ctl.Bmv2TExceptionParser.parseTException;

/**
 * Implementation of a Thrift client to control a BMv2 device.
 */
public final class Bmv2DeviceThriftClient implements Bmv2DeviceAgent {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // FIXME: make context_id arbitrary for each call
    // See: https://github.com/p4lang/behavioral-model/blob/master/modules/bm_sim/include/bm_sim/context.h
    private static final int CONTEXT_ID = 0;

    protected final Standard.Iface standardClient;
    private final SimpleSwitch.Iface simpleSwitchClient;
    private final TTransport transport;
    private final DeviceId deviceId;

    // ban constructor
    protected Bmv2DeviceThriftClient(DeviceId deviceId, TTransport transport, Standard.Iface standardClient,
                                     SimpleSwitch.Iface simpleSwitchClient) {
        this.deviceId = deviceId;
        this.transport = transport;
        this.standardClient = standardClient;
        this.simpleSwitchClient = simpleSwitchClient;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public boolean ping() {
        try {
            return this.simpleSwitchClient.ping();
        } catch (TException e) {
            return false;
        }
    }

    @Override
    public final long addTableEntry(Bmv2TableEntry entry) throws Bmv2RuntimeException {

        log.debug("Adding table entry... > deviceId={}, entry={}", deviceId, entry);

        long entryId = -1;

        try {
            BmAddEntryOptions options = new BmAddEntryOptions();

            if (entry.hasPriority()) {
                options.setPriority(entry.priority());
            }

            entryId = standardClient.bm_mt_add_entry(
                    CONTEXT_ID,
                    entry.tableName(),
                    buildMatchParamsList(entry.matchKey()),
                    entry.action().name(),
                    buildActionParamsList(entry.action()),
                    options);

            if (entry.hasTimeout()) {
                /* bmv2 accepts timeouts in milliseconds */
                int msTimeout = (int) Math.round(entry.timeout() * 1_000);
                standardClient.bm_mt_set_entry_ttl(
                        CONTEXT_ID, entry.tableName(), entryId, msTimeout);
            }

            log.debug("Table entry added! > deviceId={}, entryId={}/{}", deviceId, entry.tableName(), entryId);

            return entryId;

        } catch (TException e) {
            log.debug("Exception while adding table entry: {} > deviceId={}, tableName={}",
                      e, deviceId, entry.tableName());
            if (entryId != -1) {
                // entry is in inconsistent state (unable to add timeout), remove it
                try {
                    deleteTableEntry(entry.tableName(), entryId);
                } catch (Bmv2RuntimeException e1) {
                    log.debug("Unable to remove failed table entry: {} > deviceId={}, tableName={}",
                              e1, deviceId, entry.tableName());
                }
            }
            throw parseTException(e);
        }
    }

    @Override
    public final void modifyTableEntry(String tableName,
                                       long entryId, Bmv2Action action)
            throws Bmv2RuntimeException {

        log.debug("Modifying table entry... > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);

        try {
            standardClient.bm_mt_modify_entry(
                    CONTEXT_ID,
                    tableName,
                    entryId,
                    action.name(),
                    buildActionParamsList(action));
            log.debug("Table entry modified! > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);
        } catch (TException e) {
            log.debug("Exception while modifying table entry: {} > deviceId={}, entryId={}/{}",
                      e, deviceId, tableName, entryId);
            throw parseTException(e);
        }
    }

    @Override
    public final void deleteTableEntry(String tableName,
                                       long entryId) throws Bmv2RuntimeException {

        log.debug("Deleting table entry... > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);

        try {
            standardClient.bm_mt_delete_entry(CONTEXT_ID, tableName, entryId);
            log.debug("Table entry deleted! > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);
        } catch (TException e) {
            log.debug("Exception while deleting table entry: {} > deviceId={}, entryId={}/{}",
                      e, deviceId, tableName, entryId);
            throw parseTException(e);
        }
    }

    @Override
    public final void setTableDefaultAction(String tableName, Bmv2Action action)
            throws Bmv2RuntimeException {

        log.debug("Setting table default... > deviceId={}, tableName={}, action={}", deviceId, tableName, action);

        try {
            standardClient.bm_mt_set_default_action(
                    CONTEXT_ID,
                    tableName,
                    action.name(),
                    buildActionParamsList(action));
            log.debug("Table default set! > deviceId={}, tableName={}, action={}", deviceId, tableName, action);
        } catch (TException e) {
            log.debug("Exception while setting table default : {} > deviceId={}, tableName={}, action={}",
                      e, deviceId, tableName, action);
            throw parseTException(e);
        }
    }

    @Override
    public Collection<Bmv2PortInfo> getPortsInfo() throws Bmv2RuntimeException {

        log.debug("Retrieving port info... > deviceId={}", deviceId);

        try {
            return standardClient.bm_dev_mgr_show_ports().stream()
                    .map(p -> new Bmv2PortInfo(p.getIface_name(), p.getPort_num(), p.isIs_up()))
                    .collect(Collectors.toList());
        } catch (TException e) {
            log.debug("Exception while retrieving port info: {} > deviceId={}", e, deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public List<Bmv2ParsedTableEntry> getTableEntries(String tableName) throws Bmv2RuntimeException {

        log.debug("Retrieving table entries... > deviceId={}, tableName={}", deviceId, tableName);

        List<BmMtEntry> bmEntries;
        try {
            bmEntries = standardClient.bm_mt_get_entries(CONTEXT_ID, tableName);
        } catch (TException e) {
            log.debug("Exception while retrieving table entries: {} > deviceId={}, tableName={}",
                      e, deviceId, tableName);
            throw parseTException(e);
        }

        List<Bmv2ParsedTableEntry> parsedEntries = Lists.newArrayList();

        entryLoop:
        for (BmMtEntry bmEntry : bmEntries) {

            Bmv2MatchKey.Builder matchKeyBuilder = Bmv2MatchKey.builder();
            for (BmMatchParam bmParam : bmEntry.getMatch_key()) {
                Bmv2MatchParam param;
                switch (bmParam.getType()) {
                    case EXACT:
                        param = new Bmv2ExactMatchParam(copyFrom(bmParam.getExact().getKey()));
                        break;
                    case LPM:
                        param = new Bmv2LpmMatchParam(copyFrom(bmParam.getLpm().getKey()),
                                                      bmParam.getLpm().getPrefix_length());
                        break;
                    case TERNARY:
                        param = new Bmv2TernaryMatchParam(copyFrom(bmParam.getTernary().getKey()),
                                                          copyFrom(bmParam.getTernary().getMask()));
                        break;
                    case VALID:
                        param = new Bmv2ValidMatchParam(bmParam.getValid().isKey());
                        break;
                    default:
                        log.warn("Parsing of match type {} unsupported, skipping table entry.",
                                 bmParam.getType().name());
                        continue entryLoop;
                }
                matchKeyBuilder.add(param);
            }

            Bmv2Action.Builder actionBuilder = Bmv2Action.builder();
            BmActionEntry bmActionEntry = bmEntry.getAction_entry();
            switch (bmActionEntry.getAction_type()) {
                case ACTION_DATA:
                    actionBuilder.withName(bmActionEntry.getAction_name());
                    bmActionEntry.getAction_data()
                            .stream()
                            .map(ImmutableByteSequence::copyFrom)
                            .forEach(actionBuilder::addParameter);
                    break;
                default:
                    log.warn("Parsing of action action type {} unsupported, skipping table entry.",
                             bmActionEntry.getAction_type().name());
                    continue entryLoop;
            }

            parsedEntries.add(new Bmv2ParsedTableEntry(bmEntry.getEntry_handle(), matchKeyBuilder.build(),
                                                       actionBuilder.build(), bmEntry.getOptions().getPriority()));
        }

        return parsedEntries;
    }

    @Override
    public void transmitPacket(int portNumber, ImmutableByteSequence packet) throws Bmv2RuntimeException {

        log.debug("Requesting packet transmission... > portNumber={}, packetSize={}", portNumber, packet.size());

        try {

            simpleSwitchClient.packet_out(portNumber, ByteBuffer.wrap(packet.asArray()));
            log.debug("Packet transmission requested! > portNumber={}, packetSize={}", portNumber, packet.size());
        } catch (TException e) {
            log.debug("Exception while requesting packet transmission: {} > portNumber={}, packetSize={}",
                      e, portNumber, packet.size());
            throw parseTException(e);
        }
    }

    @Override
    public void resetState() throws Bmv2RuntimeException {

        log.debug("Resetting device state... > deviceId={}", deviceId);

        try {
            standardClient.bm_reset_state();
            log.debug("Device state reset! > deviceId={}", deviceId);
        } catch (TException e) {
            log.debug("Exception while resetting device state: {} > deviceId={}", e, deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public String dumpJsonConfig() throws Bmv2RuntimeException {

        log.debug("Dumping device config... > deviceId={}", deviceId);

        try {
            String config = standardClient.bm_get_config();
            log.debug("Device config dumped! > deviceId={}, configLength={}", deviceId, config.length());
            return config;
        } catch (TException e) {
            log.debug("Exception while dumping device config: {} > deviceId={}", e, deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public Pair<Long, Long> readTableEntryCounter(String tableName, long entryId) throws Bmv2RuntimeException {

        log.debug("Reading table entry counters... > deviceId={}, tableName={}, entryId={}",
                  deviceId, tableName, entryId);

        try {
            BmCounterValue counterValue = standardClient.bm_mt_read_counter(CONTEXT_ID, tableName, entryId);
            log.debug("Table entry counters retrieved! > deviceId={}, tableName={}, entryId={}, bytes={}, packets={}",
                      deviceId, tableName, entryId, counterValue.bytes, counterValue.packets);
            return Pair.of(counterValue.bytes, counterValue.packets);
        } catch (TException e) {
            log.debug("Exception while reading table counters: {} > deviceId={}, tableName={}, entryId={}",
                      e.toString(), deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public Pair<Long, Long> readCounter(String counterName, int index) throws Bmv2RuntimeException {

        log.debug("Reading table entry counters... > deviceId={}, counterName={}, index={}",
                  deviceId, counterName, index);

        try {
            BmCounterValue counterValue = standardClient.bm_counter_read(CONTEXT_ID, counterName, index);
            log.debug("Table entry counters retrieved! >deviceId={}, counterName={}, index={}, bytes={}, packets={}",
                      deviceId, counterName, index, counterValue.bytes, counterValue.packets);
            return Pair.of(counterValue.bytes, counterValue.packets);
        } catch (TException e) {
            log.debug("Exception while reading table counters: {} > deviceId={}, counterName={}, index={}",
                      e.toString(), deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public int getProcessInstanceId() throws Bmv2RuntimeException {
        log.debug("Getting process instance ID... > deviceId={}", deviceId);
        try {
            int instanceId = simpleSwitchClient.get_process_instance_id();
            log.debug("TProcess instance ID retrieved! > deviceId={}, instanceId={}",
                      deviceId, instanceId);
            return instanceId;
        } catch (TException e) {
            log.debug("Exception while getting process instance ID: {} > deviceId={}", e.toString(), deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public String getJsonConfigMd5() throws Bmv2RuntimeException {

        log.debug("Getting device config md5... > deviceId={}", deviceId);

        try {
            String md5 = standardClient.bm_get_config_md5();
            log.debug("Device config md5 received! > deviceId={}, configMd5={}", deviceId, md5);
            return md5;
        } catch (TException e) {
            log.debug("Exception while getting device config md5: {} > deviceId={}", e, deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public void uploadNewJsonConfig(String jsonString) throws Bmv2RuntimeException {

        log.debug("Loading new JSON config on device... > deviceId={}, jsonStringLength={}",
                  deviceId, jsonString.length());

        try {
            standardClient.bm_load_new_config(jsonString);
            log.debug("JSON config loaded! > deviceId={}", deviceId);
        } catch (TException e) {
            log.debug("Exception while loading JSON config: {} > deviceId={}", e, deviceId);
            throw parseTException(e);
        }
    }

    @Override
    public void swapJsonConfig() throws Bmv2RuntimeException {

        log.debug("Swapping JSON config on device... > deviceId={}", deviceId);

        try {
            standardClient.bm_swap_configs();
            simpleSwitchClient.force_swap();
            log.debug("JSON config swapped! > deviceId={}", deviceId);
        } catch (TException e) {
            log.debug("Exception while swapping JSON config: {} > deviceId={}", e, deviceId);
            throw parseTException(e);
        }
    }

    /**
     * Builds a list of Bmv2/Thrift compatible match parameters.
     *
     * @param matchKey a bmv2 matchKey
     * @return list of thrift-compatible bm match parameters
     */
    private static List<BmMatchParam> buildMatchParamsList(Bmv2MatchKey matchKey) {
        List<BmMatchParam> paramsList = Lists.newArrayList();
        matchKey.matchParams().forEach(x -> {
            ByteBuffer value;
            ByteBuffer mask;
            switch (x.type()) {
                case EXACT:
                    value = ByteBuffer.wrap(((Bmv2ExactMatchParam) x).value().asArray());
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.EXACT)
                                    .setExact(new BmMatchParamExact(value)));
                    break;
                case TERNARY:
                    value = ByteBuffer.wrap(((Bmv2TernaryMatchParam) x).value().asArray());
                    mask = ByteBuffer.wrap(((Bmv2TernaryMatchParam) x).mask().asArray());
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.TERNARY)
                                    .setTernary(new BmMatchParamTernary(value, mask)));
                    break;
                case LPM:
                    value = ByteBuffer.wrap(((Bmv2LpmMatchParam) x).value().asArray());
                    int prefixLength = ((Bmv2LpmMatchParam) x).prefixLength();
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.LPM)
                                    .setLpm(new BmMatchParamLPM(value, prefixLength)));
                    break;
                case VALID:
                    boolean flag = ((Bmv2ValidMatchParam) x).flag();
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.VALID)
                                    .setValid(new BmMatchParamValid(flag)));
                    break;
                default:
                    // should never be here
                    throw new RuntimeException("Unknown match param type " + x.type().name());
            }
        });
        return paramsList;
    }

    /**
     * Build a list of Bmv2/Thrift compatible action parameters.
     *
     * @param action an action object
     * @return list of ByteBuffers
     */
    private static List<ByteBuffer> buildActionParamsList(Bmv2Action action) {
        List<ByteBuffer> buffers = Lists.newArrayList();
        action.parameters().forEach(p -> buffers.add(ByteBuffer.wrap(p.asArray())));
        return buffers;
    }
}
