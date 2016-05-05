/*
 * Copyright 2014-2016 Open Networking Laboratory
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2Client;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2PortInfo;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ValidMatchParam;
import org.onosproject.net.DeviceId;
import org.p4.bmv2.thrift.BmAddEntryOptions;
import org.p4.bmv2.thrift.BmCounterValue;
import org.p4.bmv2.thrift.BmMatchParam;
import org.p4.bmv2.thrift.BmMatchParamExact;
import org.p4.bmv2.thrift.BmMatchParamLPM;
import org.p4.bmv2.thrift.BmMatchParamTernary;
import org.p4.bmv2.thrift.BmMatchParamType;
import org.p4.bmv2.thrift.BmMatchParamValid;
import org.p4.bmv2.thrift.DevMgrPortInfo;
import org.p4.bmv2.thrift.SimpleSwitch;
import org.p4.bmv2.thrift.Standard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.bmv2.ctl.SafeThriftClient.Options;

/**
 * Implementation of a Thrift client to control the Bmv2 switch.
 */
public final class Bmv2ThriftClient implements Bmv2Client {

    private static final Logger LOG =
            LoggerFactory.getLogger(Bmv2ThriftClient.class);

    // FIXME: make context_id arbitrary for each call
    // See: https://github.com/p4lang/behavioral-model/blob/master/modules/bm_sim/include/bm_sim/context.h
    private static final int CONTEXT_ID = 0;
    // Seconds after a client is expired (and connection closed) in the cache.
    private static final int CLIENT_CACHE_TIMEOUT = 60;
    // Number of connection retries after a network error.
    private static final int NUM_CONNECTION_RETRIES = 3;
    // Time between retries in milliseconds.
    private static final int TIME_BETWEEN_RETRIES = 300;

    // Static client cache where clients are removed after a predefined timeout.
    private static final LoadingCache<DeviceId, Bmv2ThriftClient>
            CLIENT_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(CLIENT_CACHE_TIMEOUT, TimeUnit.SECONDS)
            .removalListener(new ClientRemovalListener())
            .build(new ClientLoader());

    private static final Bmv2TableDumpParser TABLE_DUMP_PARSER = new Bmv2TableDumpParser();

    private final Standard.Iface standardClient;
    private final SimpleSwitch.Iface simpleSwitchClient;
    private final TTransport transport;
    private final DeviceId deviceId;

    // ban constructor
    private Bmv2ThriftClient(DeviceId deviceId, TTransport transport, Standard.Iface standardClient,
                             SimpleSwitch.Iface simpleSwitchClient) {
        this.deviceId = deviceId;
        this.transport = transport;
        this.standardClient = standardClient;
        this.simpleSwitchClient = simpleSwitchClient;

        LOG.debug("New client created! > deviceId={}", deviceId);
    }

    /**
     * Returns a client object to control the passed device.
     *
     * @param deviceId device id
     * @return bmv2 client object
     * @throws Bmv2RuntimeException if a connection to the device cannot be established
     */
    public static Bmv2ThriftClient of(DeviceId deviceId) throws Bmv2RuntimeException {
        try {
            checkNotNull(deviceId, "deviceId cannot be null");
            LOG.debug("Getting a client from cache... > deviceId{}", deviceId);
            return CLIENT_CACHE.get(deviceId);
        } catch (ExecutionException e) {
            LOG.debug("Exception while getting a client from cache: {} > ", e, deviceId);
            throw new Bmv2RuntimeException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Force a close of the transport session (if one is open) with the given device.
     *
     * @param deviceId device id
     */
    public static void forceDisconnectOf(DeviceId deviceId) {
        CLIENT_CACHE.invalidate(deviceId);
    }

    /**
     * Pings the device. Returns true if the device is reachable,
     * false otherwise.
     *
     * @param deviceId device id
     * @return true if reachable, false otherwise
     */
    public static boolean ping(DeviceId deviceId) {
        // poll ports status as workaround to assess device reachability
        try {
            LOG.debug("Pinging device... > deviceId={}", deviceId);
            Bmv2ThriftClient client = of(deviceId);
            boolean result = client.simpleSwitchClient.ping();
            LOG.debug("Device pinged! > deviceId={}, state={}", deviceId, result);
            return result;
        } catch (TException | Bmv2RuntimeException e) {
            LOG.debug("Device NOT reachable! > deviceId={}", deviceId);
            return false;
        }
    }

    /**
     * Parse device ID into host and port.
     *
     * @param did device ID
     * @return a pair of host and port
     */
    private static Pair<String, Integer> parseDeviceId(DeviceId did) {
        String[] info = did.toString().split(":");
        if (info.length == 3) {
            String host = info[1];
            int port = Integer.parseInt(info[2]);
            return ImmutablePair.of(host, port);
        } else {
            throw new IllegalArgumentException(
                    "Unable to parse BMv2 device ID "
                            + did.toString()
                            + ", expected format is scheme:host:port");
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

    @Override
    public final long addTableEntry(Bmv2TableEntry entry) throws Bmv2RuntimeException {

        LOG.debug("Adding table entry... > deviceId={}, entry={}", deviceId, entry);

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

            LOG.debug("Table entry added! > deviceId={}, entryId={}/{}", deviceId, entry.tableName(), entryId);

            return entryId;

        } catch (TException e) {
            LOG.debug("Exception while adding table entry: {} > deviceId={}, tableName={}",
                      e, deviceId, entry.tableName());
            if (entryId != -1) {
                // entry is in inconsistent state (unable to add timeout), remove it
                try {
                    deleteTableEntry(entry.tableName(), entryId);
                } catch (Bmv2RuntimeException e1) {
                    LOG.debug("Unable to remove failed table entry: {} > deviceId={}, tableName={}",
                              e1, deviceId, entry.tableName());
                }
            }
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public final void modifyTableEntry(String tableName,
                                       long entryId, Bmv2Action action)
            throws Bmv2RuntimeException {

        LOG.debug("Modifying table entry... > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);

        try {
            standardClient.bm_mt_modify_entry(
                    CONTEXT_ID,
                    tableName,
                    entryId,
                    action.name(),
                    buildActionParamsList(action));
            LOG.debug("Table entry modified! > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);
        } catch (TException e) {
            LOG.debug("Exception while modifying table entry: {} > deviceId={}, entryId={}/{}",
                      e, deviceId, tableName, entryId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public final void deleteTableEntry(String tableName,
                                       long entryId) throws Bmv2RuntimeException {

        LOG.debug("Deleting table entry... > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);

        try {
            standardClient.bm_mt_delete_entry(CONTEXT_ID, tableName, entryId);
            LOG.debug("Table entry deleted! > deviceId={}, entryId={}/{}", deviceId, tableName, entryId);
        } catch (TException e) {
            LOG.debug("Exception while deleting table entry: {} > deviceId={}, entryId={}/{}",
                      e, deviceId, tableName, entryId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public final void setTableDefaultAction(String tableName, Bmv2Action action)
            throws Bmv2RuntimeException {

        LOG.debug("Setting table default... > deviceId={}, tableName={}, action={}", deviceId, tableName, action);

        try {
            standardClient.bm_mt_set_default_action(
                    CONTEXT_ID,
                    tableName,
                    action.name(),
                    buildActionParamsList(action));
            LOG.debug("Table default set! > deviceId={}, tableName={}, action={}", deviceId, tableName, action);
        } catch (TException e) {
            LOG.debug("Exception while setting table default : {} > deviceId={}, tableName={}, action={}",
                      e, deviceId, tableName, action);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Collection<Bmv2PortInfo> getPortsInfo() throws Bmv2RuntimeException {

        LOG.debug("Retrieving port info... > deviceId={}", deviceId);

        try {
            List<DevMgrPortInfo> portInfos = standardClient.bm_dev_mgr_show_ports();

            Collection<Bmv2PortInfo> bmv2PortInfos = Lists.newArrayList();

            bmv2PortInfos.addAll(
                    portInfos.stream()
                            .map(Bmv2PortInfo::new)
                            .collect(Collectors.toList()));

            LOG.debug("Port info retrieved! > deviceId={}, portInfos={}", deviceId, bmv2PortInfos);

            return bmv2PortInfos;

        } catch (TException e) {
            LOG.debug("Exception while retrieving port info: {} > deviceId={}", e, deviceId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String dumpTable(String tableName) throws Bmv2RuntimeException {

        LOG.debug("Retrieving table dump... > deviceId={}, tableName={}", deviceId, tableName);

        try {
            String dump = standardClient.bm_dump_table(CONTEXT_ID, tableName);
            LOG.debug("Table dump retrieved! > deviceId={}, tableName={}", deviceId, tableName);
            return dump;
        } catch (TException e) {
            LOG.debug("Exception while retrieving table dump: {} > deviceId={}, tableName={}",
                      e, deviceId, tableName);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<Long> getInstalledEntryIds(String tableName) throws Bmv2RuntimeException {

        LOG.debug("Getting entry ids... > deviceId={}, tableName={}", deviceId, tableName);

        try {
            List<Long> entryIds = TABLE_DUMP_PARSER.getEntryIds(dumpTable(tableName));
            LOG.debug("Entry ids retrieved! > deviceId={}, tableName={}, entryIdsCount={}",
                      deviceId, tableName, entryIds.size());
            return entryIds;
        } catch (Bmv2TableDumpParser.Bmv2TableDumpParserException e) {
            LOG.debug("Exception while retrieving entry ids: {} > deviceId={}, tableName={}",
                      e, deviceId, tableName);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public int cleanupTable(String tableName) throws Bmv2RuntimeException {

        LOG.debug("Starting table cleanup... > deviceId={}, tableName={}", deviceId, tableName);

        List<Long> entryIds = getInstalledEntryIds(tableName);

        int count = 0;
        for (Long entryId : entryIds) {
            try {
                standardClient.bm_mt_delete_entry(CONTEXT_ID, tableName, entryId);
                count++;
            } catch (TException e) {
                LOG.warn("Exception while deleting entry: {} > deviceId={}, tableName={}, entryId={}",
                         e.toString(), deviceId, tableName, entryId);
            }
        }

        return count;
    }

    @Override
    public void transmitPacket(int portNumber, ImmutableByteSequence packet) throws Bmv2RuntimeException {

        LOG.debug("Requesting packet transmission... > portNumber={}, packet={}", portNumber, packet);

        try {

            simpleSwitchClient.push_packet(portNumber, ByteBuffer.wrap(packet.asArray()));
            LOG.debug("Packet transmission requested! > portNumber={}, packet={}", portNumber, packet);
        } catch (TException e) {
            LOG.debug("Exception while requesting packet transmission: {} > portNumber={}, packet={}",
                      e, portNumber, packet);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void resetState() throws Bmv2RuntimeException {

        LOG.debug("Resetting device state... > deviceId={}", deviceId);

        try {
            standardClient.bm_reset_state();
            LOG.debug("Device state reset! > deviceId={}", deviceId);
        } catch (TException e) {
            LOG.debug("Exception while resetting device state: {} > deviceId={}", e, deviceId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String dumpJsonConfig() throws Bmv2RuntimeException {

        LOG.debug("Dumping device config... > deviceId={}", deviceId);

        try {
            String config = standardClient.bm_get_config();
            LOG.debug("Device config dumped! > deviceId={}, configLength={}", deviceId, config.length());
            return config;
        } catch (TException e) {
            LOG.debug("Exception while dumping device config: {} > deviceId={}", e, deviceId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Pair<Long, Long> readTableEntryCounter(String tableName, long entryId) throws Bmv2RuntimeException {

        LOG.debug("Reading table entry counters... > deviceId={}, tableName={}, entryId={}",
                  deviceId, tableName, entryId);

        try {
            BmCounterValue counterValue = standardClient.bm_mt_read_counter(CONTEXT_ID, tableName, entryId);
            LOG.debug("Table entry counters retrieved! > deviceId={}, tableName={}, entryId={}, bytes={}, packets={}",
                      deviceId, tableName, entryId, counterValue.bytes, counterValue.packets);
            return Pair.of(counterValue.bytes, counterValue.packets);
        } catch (TException e) {
            LOG.debug("Exception while reading table counters: {} > deviceId={}, tableName={}, entryId={}",
                      e.toString(), deviceId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String getJsonConfigMd5() throws Bmv2RuntimeException {

        LOG.debug("Getting device config md5... > deviceId={}", deviceId);

        try {
            String md5 = standardClient.bm_get_config_md5();
            LOG.debug("Device config md5 received! > deviceId={}, configMd5={}", deviceId, md5);
            return md5;
        } catch (TException e) {
            LOG.debug("Exception while getting device config md5: {} > deviceId={}", e, deviceId);
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Transport/client cache loader.
     */
    private static class ClientLoader
            extends CacheLoader<DeviceId, Bmv2ThriftClient> {

        private static final Options RECONN_OPTIONS = new Options(NUM_CONNECTION_RETRIES, TIME_BETWEEN_RETRIES);

        @Override
        public Bmv2ThriftClient load(DeviceId deviceId)
                throws TTransportException {
            LOG.debug("Creating new client in cache... > deviceId={}", deviceId);
            Pair<String, Integer> info = parseDeviceId(deviceId);
            //make the expensive call
            TTransport transport = new TSocket(
                    info.getLeft(), info.getRight());
            TProtocol protocol = new TBinaryProtocol(transport);
            // Our BMv2 device implements multiple Thrift services, create a client for each one on the same transport.
            Standard.Client standardClient = new Standard.Client(
                    new TMultiplexedProtocol(protocol, "standard"));
            SimpleSwitch.Client simpleSwitch = new SimpleSwitch.Client(
                    new TMultiplexedProtocol(protocol, "simple_switch"));
            // Wrap clients so to automatically have synchronization and resiliency to connectivity errors
            Standard.Iface safeStandardClient = SafeThriftClient.wrap(standardClient,
                                                                      Standard.Iface.class,
                                                                      RECONN_OPTIONS);
            SimpleSwitch.Iface safeSimpleSwitchClient = SafeThriftClient.wrap(simpleSwitch,
                                                                              SimpleSwitch.Iface.class,
                                                                              RECONN_OPTIONS);

            return new Bmv2ThriftClient(deviceId, transport, safeStandardClient, safeSimpleSwitchClient);
        }
    }

    /**
     * Client cache removal listener. Close the connection on cache removal.
     */
    private static class ClientRemovalListener implements
            RemovalListener<DeviceId, Bmv2ThriftClient> {

        @Override
        public void onRemoval(RemovalNotification<DeviceId, Bmv2ThriftClient> notification) {
            // close the transport connection
            Bmv2ThriftClient client = notification.getValue();
            // Locking here is ugly, but needed (see SafeThriftClient).
            synchronized (client.transport) {
                LOG.debug("Closing transport session... > deviceId={}", client.deviceId);
                if (client.transport.isOpen()) {
                    client.transport.close();
                    LOG.debug("Transport session closed! > deviceId={}", client.deviceId);
                } else {
                    LOG.debug("Transport session was already closed! deviceId={}", client.deviceId);
                }
            }
            LOG.debug("Removing client from cache... > deviceId={}", client.deviceId);
        }
    }
}
