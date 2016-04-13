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
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2PortInfo;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ValidMatchParam;
import org.onosproject.net.DeviceId;
import org.p4.bmv2.thrift.BmAddEntryOptions;
import org.p4.bmv2.thrift.BmMatchParam;
import org.p4.bmv2.thrift.BmMatchParamExact;
import org.p4.bmv2.thrift.BmMatchParamLPM;
import org.p4.bmv2.thrift.BmMatchParamTernary;
import org.p4.bmv2.thrift.BmMatchParamType;
import org.p4.bmv2.thrift.BmMatchParamValid;
import org.p4.bmv2.thrift.DevMgrPortInfo;
import org.p4.bmv2.thrift.Standard;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of a Thrift client to control the Bmv2 switch.
 */
public final class Bmv2ThriftClient {
    /*
    FIXME: derive context_id from device id
    Using different context id values should serve to control different
    switches responding to the same IP address and port
    */
    private static final int CONTEXT_ID = 0;
    /*
    Static transport/client cache:
        - avoids opening a new transport session when there's one already open
        - close the connection after a predefined timeout of 5 seconds
     */
    private static LoadingCache<DeviceId, Bmv2ThriftClient>
            clientCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .removalListener(new ClientRemovalListener())
            .build(new ClientLoader());
    private final Standard.Iface stdClient;
    private final TTransport transport;

    // ban constructor
    private Bmv2ThriftClient(TTransport transport, Standard.Iface stdClient) {
        this.transport = transport;
        this.stdClient = stdClient;
    }

    private void closeTransport() {
        this.transport.close();
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
            return clientCache.get(deviceId);
        } catch (ExecutionException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e.getCause());
        }
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
            of(deviceId).stdClient.bm_dev_mgr_show_ports();
            return true;
        } catch (TException | Bmv2RuntimeException e) {
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
            switch (x.type()) {
                case EXACT:
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.EXACT)
                                    .setExact(new BmMatchParamExact(
                                            ((Bmv2ExactMatchParam) x).value().asReadOnlyBuffer())));
                    break;
                case TERNARY:
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.TERNARY)
                                    .setTernary(new BmMatchParamTernary(
                                            ((Bmv2TernaryMatchParam) x).value().asReadOnlyBuffer(),
                                            ((Bmv2TernaryMatchParam) x).mask().asReadOnlyBuffer())));
                    break;
                case LPM:
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.LPM)
                                    .setLpm(new BmMatchParamLPM(
                                            ((Bmv2LpmMatchParam) x).value().asReadOnlyBuffer(),
                                            ((Bmv2LpmMatchParam) x).prefixLength())));
                    break;
                case VALID:
                    paramsList.add(
                            new BmMatchParam(BmMatchParamType.VALID)
                                    .setValid(new BmMatchParamValid(
                                            ((Bmv2ValidMatchParam) x).flag())));
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
        return action.parameters()
                .stream()
                .map(ImmutableByteSequence::asReadOnlyBuffer)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new table entry.
     *
     * @param entry a table entry value
     * @return table-specific entry ID
     * @throws Bmv2RuntimeException if any error occurs
     */
    public final long addTableEntry(Bmv2TableEntry entry) throws Bmv2RuntimeException {

        long entryId = -1;

        try {
            BmAddEntryOptions options = new BmAddEntryOptions();

            if (entry.hasPriority()) {
                options.setPriority(entry.priority());
            }

            entryId = stdClient.bm_mt_add_entry(
                    CONTEXT_ID,
                    entry.tableName(),
                    buildMatchParamsList(entry.matchKey()),
                    entry.action().name(),
                    buildActionParamsList(entry.action()),
                    options);

            if (entry.hasTimeout()) {
                /* bmv2 accepts timeouts in milliseconds */
                int msTimeout = (int) Math.round(entry.timeout() * 1_000);
                stdClient.bm_mt_set_entry_ttl(
                        CONTEXT_ID, entry.tableName(), entryId, msTimeout);
            }

            return entryId;

        } catch (TException e) {
            if (entryId != -1) {
                try {
                    stdClient.bm_mt_delete_entry(
                            CONTEXT_ID, entry.tableName(), entryId);
                } catch (TException e1) {
                    // this should never happen as we know the entry is there
                    throw new Bmv2RuntimeException(e1.getMessage(), e1);
                }
            }
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Modifies a currently installed entry by updating its action.
     *
     * @param tableName string value of table name
     * @param entryId   long value of entry ID
     * @param action    an action value
     * @throws Bmv2RuntimeException if any error occurs
     */
    public final void modifyTableEntry(String tableName,
                                       long entryId, Bmv2Action action)
            throws Bmv2RuntimeException {

        try {
            stdClient.bm_mt_modify_entry(
                    CONTEXT_ID,
                    tableName,
                    entryId,
                    action.name(),
                    buildActionParamsList(action));
        } catch (TException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Deletes currently installed entry.
     *
     * @param tableName string value of table name
     * @param entryId   long value of entry ID
     * @throws Bmv2RuntimeException if any error occurs
     */
    public final void deleteTableEntry(String tableName,
                                       long entryId) throws Bmv2RuntimeException {

        try {
            stdClient.bm_mt_delete_entry(CONTEXT_ID, tableName, entryId);
        } catch (TException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Sets table default action.
     *
     * @param tableName string value of table name
     * @param action    an action value
     * @throws Bmv2RuntimeException if any error occurs
     */
    public final void setTableDefaultAction(String tableName, Bmv2Action action)
            throws Bmv2RuntimeException {

        try {
            stdClient.bm_mt_set_default_action(
                    CONTEXT_ID,
                    tableName,
                    action.name(),
                    buildActionParamsList(action));
        } catch (TException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns information of the ports currently configured in the switch.
     *
     * @return collection of port information
     * @throws Bmv2RuntimeException if any error occurs
     */
    public Collection<Bmv2PortInfo> getPortsInfo() throws Bmv2RuntimeException {

        try {
            List<DevMgrPortInfo> portInfos = stdClient.bm_dev_mgr_show_ports();

            Collection<Bmv2PortInfo> bmv2PortInfos = Lists.newArrayList();

            bmv2PortInfos.addAll(
                    portInfos.stream()
                            .map(Bmv2PortInfo::new)
                            .collect(Collectors.toList()));

            return bmv2PortInfos;

        } catch (TException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Return a string representation of a table content.
     *
     * @param tableName string value of table name
     * @return table string dump
     * @throws Bmv2RuntimeException if any error occurs
     */
    public String dumpTable(String tableName) throws Bmv2RuntimeException {

        try {
            return stdClient.bm_dump_table(CONTEXT_ID, tableName);
        } catch (TException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Reset the state of the switch (e.g. delete all entries, etc.).
     *
     * @throws Bmv2RuntimeException if any error occurs
     */
    public void resetState() throws Bmv2RuntimeException {

        try {
            stdClient.bm_reset_state();
        } catch (TException e) {
            throw new Bmv2RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Transport/client cache loader.
     */
    private static class ClientLoader
            extends CacheLoader<DeviceId, Bmv2ThriftClient> {

        @Override
        public Bmv2ThriftClient load(DeviceId deviceId)
                throws TTransportException {
            Pair<String, Integer> info = parseDeviceId(deviceId);
            //make the expensive call
            TTransport transport = new TSocket(
                    info.getLeft(), info.getRight());
            TProtocol protocol = new TBinaryProtocol(transport);
            Standard.Iface stdClient = new Standard.Client(
                    new TMultiplexedProtocol(protocol, "standard"));

            transport.open();

            return new Bmv2ThriftClient(transport, stdClient);
        }
    }

    /**
     * Client cache removal listener. Close the connection on cache removal.
     */
    private static class ClientRemovalListener implements
            RemovalListener<DeviceId, Bmv2ThriftClient> {

        @Override
        public void onRemoval(
                RemovalNotification<DeviceId, Bmv2ThriftClient> notification) {
            // close the transport connection
            notification.getValue().closeTransport();
        }
    }
}
