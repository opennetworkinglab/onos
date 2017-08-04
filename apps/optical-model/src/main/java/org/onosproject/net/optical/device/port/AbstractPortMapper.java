/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical.device.port;

import java.util.Optional;

import org.onosproject.net.Port;

import com.google.common.annotations.Beta;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * PortMapper which caches mapped Port instance.
 */
@Beta
public abstract class AbstractPortMapper<P extends Port> implements PortMapper<P> {

    private final LoadingCache<Port, Optional<P>> cache
            = CacheBuilder.newBuilder()
                .weakKeys() // use == to compare keys
                .maximumSize(100)
                .build(CacheLoader.from(this::mapPort));

    /**
     * {@inheritDoc}
     *
     * <p>
     * Note: Subclasses should override and implement short-cut conditions
     * and call {@code super.is(port)}.
     */
    @Override
    public boolean is(Port port) {
        return as(port).isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Note: Subclasses should override and check if {@code port} is
     * already of type {@code P} and directly return {@code Optional.of((P) port)},
     * if not call {@code super.as(port)}.
     */
    @Override
    public Optional<P> as(Port port) {
        if (port == null) {
            return Optional.empty();
        }
        return cache.getUnchecked(port);
    }

    /**
     * Returns {@code port} mapped to {@code <P>}.
     *
     * @param port Port to map
     * @return {@code port} mapped to {@code <P>}
     */
    protected abstract Optional<P> mapPort(Port port);

}
