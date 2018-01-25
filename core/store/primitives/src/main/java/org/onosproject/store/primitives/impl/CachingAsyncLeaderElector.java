/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.primitives.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.service.AsyncLeaderElector;

/**
 * Caching async leader elector.
 */
public class CachingAsyncLeaderElector extends DelegatingAsyncLeaderElector {
    private final LoadingCache<String, CompletableFuture<Leadership>> cache;
    private final Consumer<Change<Leadership>> cacheUpdater;
    private final Consumer<Status> statusListener;

    public CachingAsyncLeaderElector(AsyncLeaderElector delegateLeaderElector) {
        super(delegateLeaderElector);
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(CacheLoader.from(super::getLeadership));

        cacheUpdater = change -> {
            Leadership leadership = change.newValue();
            cache.put(leadership.topic(), CompletableFuture.completedFuture(leadership));
        };
        statusListener = status -> {
            if (status == Status.SUSPENDED || status == Status.INACTIVE) {
                cache.invalidateAll();
            }
        };
        addChangeListener(cacheUpdater);
        addStatusChangeListener(statusListener);
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return cache.getUnchecked(topic)
            .whenComplete((r, e) -> {
                if (e != null) {
                    cache.invalidate(topic);
                }
            });
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return super.run(topic, nodeId).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return super.withdraw(topic).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return super.anoint(topic, nodeId).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return super.promote(topic, nodeId).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return super.evict(nodeId).whenComplete((r, e) -> cache.invalidateAll());
    }

    @Override
    public CompletableFuture<Void> destroy() {
        removeStatusChangeListener(statusListener);
        return removeChangeListener(cacheUpdater);
    }
}
