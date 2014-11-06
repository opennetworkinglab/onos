package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kuujo.copycat.Command;
import net.kuujo.copycat.Query;
import net.kuujo.copycat.StateMachine;

import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.VersionedValue;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * StateMachine whose transitions are coordinated/replicated
 * by Raft consensus.
 * Each Raft cluster member has a instance of this state machine that is
 * independently updated in lock step once there is consensus
 * on the next transition.
 */
public class DatabaseStateMachine implements StateMachine {

    private final Logger log = getLogger(getClass());

    public static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(VersionedValue.class)
                    .register(State.class)
                    .register(ClusterMessagingProtocol.COMMON)
                    .build()
                    .populate(1);
        }
    };

    private State state = new State();

    @Command
    public boolean createTable(String tableName) {
        return state.getTables().putIfAbsent(tableName, Maps.newHashMap()) == null;
    }

    @Command
    public boolean dropTable(String tableName) {
        return state.getTables().remove(tableName) != null;
    }

    @Command
    public boolean dropAllTables() {
        state.getTables().clear();
        return true;
    }

    @Query
    public List<String> listTables() {
        return ImmutableList.copyOf(state.getTables().keySet());
    }

    @Query
    public List<InternalReadResult> read(List<ReadRequest> requests) {
        List<InternalReadResult> results = new ArrayList<>(requests.size());
        for (ReadRequest request : requests) {
            Map<String, VersionedValue> table = state.getTables().get(request.tableName());
            if (table == null) {
                results.add(new InternalReadResult(InternalReadResult.Status.NO_SUCH_TABLE, null));
                continue;
            }
            VersionedValue value = table.get(request.key());
            results.add(new InternalReadResult(
                    InternalReadResult.Status.OK,
                    new ReadResult(
                            request.tableName(),
                            request.key(),
                            value)));
        }
        return results;
    }

    @Command
    public List<InternalWriteResult> write(List<WriteRequest> requests) {
        boolean abort = false;
        List<InternalWriteResult.Status> validationResults = new ArrayList<>(requests.size());
        for (WriteRequest request : requests) {
            Map<String, VersionedValue> table = state.getTables().get(request.tableName());
            if (table == null) {
                validationResults.add(InternalWriteResult.Status.NO_SUCH_TABLE);
                abort = true;
                continue;
            }
            VersionedValue value = table.get(request.key());
            if (value == null) {
                if (request.oldValue() != null) {
                    validationResults.add(InternalWriteResult.Status.PREVIOUS_VALUE_MISMATCH);
                    abort = true;
                    continue;
                } else if (request.previousVersion() >= 0) {
                    validationResults.add(InternalWriteResult.Status.OPTIMISTIC_LOCK_FAILURE);
                    abort = true;
                    continue;
                }
            }
            if (request.previousVersion() >= 0 && value.version() != request.previousVersion()) {
                validationResults.add(InternalWriteResult.Status.OPTIMISTIC_LOCK_FAILURE);
                abort = true;
                continue;
            }

            validationResults.add(InternalWriteResult.Status.OK);
        }

        List<InternalWriteResult> results = new ArrayList<>(requests.size());

        if (abort) {
            for (InternalWriteResult.Status validationResult : validationResults) {
                if (validationResult == InternalWriteResult.Status.OK) {
                    results.add(new InternalWriteResult(InternalWriteResult.Status.ABORTED, null));
                } else {
                    results.add(new InternalWriteResult(validationResult, null));
                }
            }
            return results;
        }

        for (WriteRequest request : requests) {
            Map<String, VersionedValue> table = state.getTables().get(request.tableName());
            synchronized (table) {
                VersionedValue previousValue =
                        table.put(request.key(), new VersionedValue(request.newValue(), state.nextVersion()));
                results.add(new InternalWriteResult(
                        InternalWriteResult.Status.OK,
                        new WriteResult(request.tableName(), request.key(), previousValue)));
            }
        }
        return results;
    }

    public class State {

        private final Map<String, Map<String, VersionedValue>> tables =
                Maps.newHashMap();
        private long versionCounter = 1;

        Map<String, Map<String, VersionedValue>> getTables() {
            return tables;
        }

        long nextVersion() {
            return versionCounter++;
        }
    }

    @Override
    public byte[] takeSnapshot() {
        try {
            return SERIALIZER.encode(state);
        } catch (Exception e) {
            log.error("Snapshot serialization error", e);
            return null;
        }
    }

    @Override
    public void installSnapshot(byte[] data) {
        try {
            this.state = SERIALIZER.decode(data);
        } catch (Exception e) {
            log.error("Snapshot deserialization error", e);
        }
    }
}
