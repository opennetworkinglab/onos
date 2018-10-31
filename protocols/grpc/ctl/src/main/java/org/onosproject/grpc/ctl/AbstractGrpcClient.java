/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.grpc.ctl;

import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import org.onlab.util.SharedExecutors;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract client for gRPC service.
 *
 */
public abstract class AbstractGrpcClient implements GrpcClient {

    // Timeout in seconds to obtain the request lock.
    private static final int LOCK_TIMEOUT = 60;
    private static final int DEFAULT_THREAD_POOL_SIZE = 10;

    protected final Logger log = getLogger(getClass());

    private final Lock requestLock = new ReentrantLock();
    private final Context.CancellableContext cancellableContext =
            Context.current().withCancellation();
    private final Executor contextExecutor;

    protected final ExecutorService executorService;
    protected final DeviceId deviceId;

    protected AbstractGrpcClient(GrpcClientKey clientKey) {
        this.deviceId = clientKey.deviceId();
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE, groupedThreads(
                "onos-grpc-" + clientKey.serviceName() + "-client-" + deviceId.toString(), "%d"));
        this.contextExecutor = this.cancellableContext.fixedContextExecutor(executorService);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return supplyWithExecutor(this::doShutdown, "shutdown",
                SharedExecutors.getPoolThreadExecutor());
    }

    protected Void doShutdown() {
        log.debug("Shutting down client for {}...", deviceId);
        cancellableContext.cancel(new InterruptedException(
                "Requested client shutdown"));
        this.executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Executor service didn't shutdown in time.");
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Equivalent of supplyWithExecutor using the gRPC context executor of this
     * client, such that if the context is cancelled (e.g. client shutdown) the
     * RPC is automatically cancelled.
     *
     * @param <U> return type of supplier
     * @param supplier the supplier to be executed
     * @param opDescription the description of this supplier
     * @return CompletableFuture includes the result of supplier
     */
    protected  <U> CompletableFuture<U> supplyInContext(
            Supplier<U> supplier, String opDescription) {
        return supplyWithExecutor(supplier, opDescription, contextExecutor);
    }

    /**
     * Submits a task for async execution via the given executor. All tasks
     * submitted with this method will be executed sequentially.
     *
     * @param <U> return type of supplier
     * @param supplier the supplier to be executed
     * @param opDescription the description of this supplier
     * @param executor the executor to execute this supplier
     * @return CompletableFuture includes the result of supplier
     */
    private <U> CompletableFuture<U> supplyWithExecutor(
            Supplier<U> supplier, String opDescription, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: explore a more relaxed locking strategy.
            try {
                if (!requestLock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                    log.error("LOCK TIMEOUT! This is likely a deadlock, "
                                    + "please debug (executing {})",
                            opDescription);
                    throw new IllegalThreadStateException("Lock timeout");
                }
            } catch (InterruptedException e) {
                log.warn("Thread interrupted while waiting for lock (executing {})",
                        opDescription);
                throw new IllegalStateException(e);
            }
            try {
                return supplier.get();
            } catch (StatusRuntimeException ex) {
                log.warn("Unable to execute {} on {}: {}",
                        opDescription, deviceId, ex.toString());
                throw ex;
            } catch (Throwable ex) {
                log.error("Exception in client of {}, executing {}",
                        deviceId, opDescription, ex);
                throw ex;
            } finally {
                requestLock.unlock();
            }
        }, executor);
    }
}
