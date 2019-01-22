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

package org.onosproject.p4runtime.ctl.client;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.protobuf.TextFormat;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.ctl.codec.CodecException;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles creation of ReadResponse by parsing Read RPC server responses.
 */
public final class ReadResponseImpl implements P4RuntimeReadClient.ReadResponse {

    private static final Logger log = getLogger(ReadResponseImpl.class);

    public static final ReadResponseImpl EMPTY = new ReadResponseImpl(
            true, ImmutableList.of(), ImmutableListMultimap.of(), null, null);

    private final boolean success;
    private final ImmutableList<PiEntity> entities;
    private final ImmutableListMultimap<Class<? extends PiEntity>, PiEntity> typeToEntities;
    private final String explanation;
    private final Throwable throwable;

    private ReadResponseImpl(
            boolean success, ImmutableList<PiEntity> entities,
            ImmutableListMultimap<Class<? extends PiEntity>, PiEntity> typeToEntities,
            String explanation, Throwable throwable) {
        this.success = success;
        this.entities = entities;
        this.typeToEntities = typeToEntities;
        this.explanation = explanation;
        this.throwable = throwable;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Collection<PiEntity> all() {
        return entities;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends PiEntity> Collection<E> all(Class<E> clazz) {
        return (ImmutableList<E>) typeToEntities.get(clazz);
    }

    @Override
    public String explanation() {
        return explanation;
    }

    @Override
    public Throwable throwable() {
        return throwable;
    }

    static Builder builder(DeviceId deviceId, PiPipeconf pipeconf) {
        return new Builder(deviceId, pipeconf);
    }

    /**
     * Builder of P4RuntimeReadResponseImpl.
     */
    static final class Builder {

        private final DeviceId deviceId;
        private final PiPipeconf pipeconf;
        private final List<PiEntity> entities = Lists.newArrayList();
        private final ListMultimap<Class<? extends PiEntity>, PiEntity>
                typeToEntities = ArrayListMultimap.create();

        private boolean success = true;
        private String explanation;
        private Throwable throwable;

        private Builder(DeviceId deviceId, PiPipeconf pipeconf) {
            this.deviceId = deviceId;
            this.pipeconf = pipeconf;
        }

        void addEntity(P4RuntimeOuterClass.Entity entityMsg) {
            try {
                final PiEntity piEntity = CODECS.entity().decode(entityMsg, null, pipeconf);
                entities.add(piEntity);
                typeToEntities.put(piEntity.getClass(), piEntity);
            } catch (CodecException e) {
                log.warn("Unable to decode {} message from {}: {} [{}]",
                         entityMsg.getEntityCase().name(), deviceId,
                          e.getMessage(), TextFormat.shortDebugString(entityMsg));
            }
        }

        ReadResponseImpl fail(Throwable throwable) {
            checkNotNull(throwable);
            this.success = false;
            this.explanation = throwable.getMessage();
            this.throwable = throwable;
            return build();
        }

        ReadResponseImpl build() {
            if (success && entities.isEmpty()) {
                return EMPTY;
            }
            return new ReadResponseImpl(
                    success, ImmutableList.copyOf(entities),
                    ImmutableListMultimap.copyOf(typeToEntities),
                    explanation, throwable);
        }
    }
}
