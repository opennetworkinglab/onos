package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.onlab.onos.store.service.impl.InternalWriteResult.Status;
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

    // serializer used for snapshot
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
            VersionedValue value = VersionedValue.copy(table.get(request.key()));
            results.add(new InternalReadResult(
                    InternalReadResult.Status.OK,
                    new ReadResult(
                            request.tableName(),
                            request.key(),
                            value)));
        }
        return results;
    }

    InternalWriteResult.Status checkIfApplicable(WriteRequest request,
                                                 VersionedValue value) {

        switch (request.type()) {
        case PUT:
            return InternalWriteResult.Status.OK;

        case PUT_IF_ABSENT:
            if (value == null) {
                return InternalWriteResult.Status.OK;
            }
            return InternalWriteResult.Status.PREVIOUS_VALUE_MISMATCH;
        case PUT_IF_VALUE:
        case REMOVE_IF_VALUE:
            if (value != null && Arrays.equals(value.value(), request.oldValue())) {
                return InternalWriteResult.Status.OK;
            }
            return InternalWriteResult.Status.PREVIOUS_VALUE_MISMATCH;
        case PUT_IF_VERSION:
        case REMOVE_IF_VERSION:
            if (value != null && request.previousVersion() == value.version()) {
                return InternalWriteResult.Status.OK;
            }
            return InternalWriteResult.Status.PREVIOUS_VERSION_MISMATCH;
        case REMOVE:
            return InternalWriteResult.Status.OK;
        default:
            break;
        }
        log.error("Should never reach here {}", request);
        return InternalWriteResult.Status.ABORTED;
    }

    @Command
    public List<InternalWriteResult> write(List<WriteRequest> requests) {

        // applicability check
        boolean abort = false;
        List<InternalWriteResult.Status> validationResults = new ArrayList<>(requests.size());
        for (WriteRequest request : requests) {
            Map<String, VersionedValue> table = state.getTables().get(request.tableName());
            if (table == null) {
                validationResults.add(InternalWriteResult.Status.NO_SUCH_TABLE);
                abort = true;
                continue;
            }
            final VersionedValue value = table.get(request.key());
            Status result = checkIfApplicable(request, value);
            validationResults.add(result);
            if (result != Status.OK) {
                abort = true;
            }
        }

        List<InternalWriteResult> results = new ArrayList<>(requests.size());

        if (abort) {
            for (InternalWriteResult.Status validationResult : validationResults) {
                if (validationResult == InternalWriteResult.Status.OK) {
                    // aborted due to applicability check failure on other request
                    results.add(new InternalWriteResult(InternalWriteResult.Status.ABORTED, null));
                } else {
                    results.add(new InternalWriteResult(validationResult, null));
                }
            }
            return results;
        }

        // apply changes
        for (WriteRequest request : requests) {
            Map<String, VersionedValue> table = state.getTables().get(request.tableName());
            // FIXME: If this method could be called by multiple thread,
            // synchronization scope is wrong.
            // Whole function including applicability check needs to be protected.
            // Confirm copycat's thread safety requirement for StateMachine
            // TODO: If we need isolation, we need to block reads also
            synchronized (table) {
                switch (request.type()) {
                case PUT:
                case PUT_IF_ABSENT:
                case PUT_IF_VALUE:
                case PUT_IF_VERSION:
                    VersionedValue newValue = new VersionedValue(request.newValue(), state.nextVersion());
                    VersionedValue previousValue = table.put(request.key(), newValue);
                    WriteResult putResult = new WriteResult(request.tableName(), request.key(), previousValue);
                    results.add(InternalWriteResult.ok(putResult));
                    break;

                case REMOVE:
                case REMOVE_IF_VALUE:
                case REMOVE_IF_VERSION:
                    VersionedValue removedValue = table.remove(request.key());
                    WriteResult removeResult = new WriteResult(request.tableName(), request.key(), removedValue);
                    results.add(InternalWriteResult.ok(removeResult));
                    break;

                default:
                    log.error("Invalid WriteRequest type {}", request.type());
                    break;
                }
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
            log.error("Failed to take snapshot", e);
            throw new SnapshotException(e);
        }
    }

    @Override
    public void installSnapshot(byte[] data) {
        try {
            this.state = SERIALIZER.decode(data);
        } catch (Exception e) {
            log.error("Failed to install from snapshot", e);
            throw new SnapshotException(e);
        }
    }
}
