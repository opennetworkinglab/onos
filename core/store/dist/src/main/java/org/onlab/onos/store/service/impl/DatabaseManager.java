package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.kuujo.copycat.Copycat;
import net.kuujo.copycat.StateMachine;
import net.kuujo.copycat.cluster.TcpCluster;
import net.kuujo.copycat.cluster.TcpClusterConfig;
import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.log.ChronicleLog;
import net.kuujo.copycat.log.Log;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.netty.Endpoint;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.store.service.DatabaseAdminService;
import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.NoSuchTableException;
import org.onlab.onos.store.service.OptimisticLockException;
import org.onlab.onos.store.service.OptionalResult;
import org.onlab.onos.store.service.PreconditionFailedException;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.WriteAborted;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * Strongly consistent and durable state management service based on
 * Copycat implementation of Raft consensus protocol.
 */
@Component(immediate = true)
@Service
public class DatabaseManager implements DatabaseService, DatabaseAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    ClusterService clusterService;

    public static final String LOG_FILE_PREFIX = "onos-copy-cat-log";

    private Copycat copycat;
    private DatabaseClient client;

    @Activate
    public void activate() {
        TcpMember localMember =
                new TcpMember(
                        clusterService.getLocalNode().ip().toString(),
                        clusterService.getLocalNode().tcpPort());
        List<TcpMember> remoteMembers = Lists.newArrayList();

        for (ControllerNode node : clusterService.getNodes()) {
            TcpMember member = new TcpMember(node.ip().toString(), node.tcpPort());
            if (!member.equals(localMember)) {
                remoteMembers.add(member);
            }
        }

        // Configure the cluster.
        TcpClusterConfig config = new TcpClusterConfig();

        config.setLocalMember(localMember);
        config.setRemoteMembers(remoteMembers.toArray(new TcpMember[]{}));

        // Create the cluster.
        TcpCluster cluster = new TcpCluster(config);

        StateMachine stateMachine = new DatabaseStateMachine();
        ControllerNode thisNode = clusterService.getLocalNode();
        Log consensusLog = new ChronicleLog(LOG_FILE_PREFIX + "_" + thisNode.id());

        copycat = new Copycat(stateMachine, consensusLog, cluster, new NettyProtocol());
        copycat.start();

        client = new DatabaseClient(new Endpoint(localMember.host(), localMember.port()));

        log.info("Started.");
    }

    @Activate
    public void deactivate() {
        copycat.stop();
    }

    @Override
    public boolean createTable(String name) {
        return client.createTable(name);
    }

    @Override
    public void dropTable(String name) {
        client.dropTable(name);
    }

    @Override
    public void dropAllTables() {
        client.dropAllTables();
    }

    @Override
    public List<String> listTables() {
        return client.listTables();
    }

    @Override
    public ReadResult read(ReadRequest request) {
        return batchRead(Arrays.asList(request)).get(0).get();
    }

    @Override
    public List<OptionalResult<ReadResult, DatabaseException>> batchRead(
            List<ReadRequest> batch) {
        List<OptionalResult<ReadResult, DatabaseException>> readResults = new ArrayList<>(batch.size());
        for (InternalReadResult internalReadResult : client.batchRead(batch)) {
            if (internalReadResult.status() == InternalReadResult.Status.NO_SUCH_TABLE) {
                readResults.add(new DatabaseOperationResult<ReadResult, DatabaseException>(
                        new NoSuchTableException()));
            } else {
                readResults.add(new DatabaseOperationResult<ReadResult, DatabaseException>(
                        internalReadResult.result()));
            }
        }
        return readResults;
    }

    @Override
    public WriteResult write(WriteRequest request) {
        return batchWrite(Arrays.asList(request)).get(0).get();
    }

    @Override
    public List<OptionalResult<WriteResult, DatabaseException>> batchWrite(
            List<WriteRequest> batch) {
        List<OptionalResult<WriteResult, DatabaseException>> writeResults = new ArrayList<>(batch.size());
        for (InternalWriteResult internalWriteResult : client.batchWrite(batch)) {
            if (internalWriteResult.status() == InternalWriteResult.Status.NO_SUCH_TABLE) {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new NoSuchTableException()));
            } else if (internalWriteResult.status() == InternalWriteResult.Status.OPTIMISTIC_LOCK_FAILURE) {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new OptimisticLockException()));
            } else if (internalWriteResult.status() == InternalWriteResult.Status.PREVIOUS_VALUE_MISMATCH) {
                // TODO: throw a different exception?
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new PreconditionFailedException()));
            } else if (internalWriteResult.status() == InternalWriteResult.Status.ABORTED) {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new WriteAborted()));
            } else {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        internalWriteResult.result()));
            }
        }
        return writeResults;

    }

    private class DatabaseOperationResult<R, E extends DatabaseException> implements OptionalResult<R, E> {

        private final R result;
        private final DatabaseException exception;

        public DatabaseOperationResult(R result) {
            this.result = result;
            this.exception = null;
        }

        public DatabaseOperationResult(DatabaseException exception) {
            this.result = null;
            this.exception = exception;
        }

        @Override
        public R get() {
            if (result != null) {
                return result;
            }
            throw exception;
        }

        @Override
        public boolean hasValidResult() {
            return result != null;
        }

        @Override
        public String toString() {
            if (result != null) {
                return result.toString();
            } else {
                return exception.toString();
            }
        }
    }
}
