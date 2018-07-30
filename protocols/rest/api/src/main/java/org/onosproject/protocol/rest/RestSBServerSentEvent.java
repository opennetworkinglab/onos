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

package org.onosproject.protocol.rest;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import javax.ws.rs.sse.InboundSseEvent;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event received on the REST SB interface as ServerSentEvent (SSE_INBOUND).
 */
public class RestSBServerSentEvent extends AbstractEvent<RestSBServerSentEvent.Type, DeviceId> {

    private String id;
    private String comment;
    private String data;
    private String name;

    /**
     * SSE Event types supported.
     */
    public enum Type {
        SSE_INBOUND
    }

    public RestSBServerSentEvent(Type type, DeviceId deviceId, InboundSseEvent sseEvent) {
        super(type, deviceId);
        checkNotNull(sseEvent);
        data = sseEvent.readData();
        id = sseEvent.getId();
        name = sseEvent.getName();
        comment = sseEvent.getComment();
    }

    public String getData() {
        return data;
    }

    public String getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString() + ", id=" + id + ", name=" + name + ", comment=" + comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestSBServerSentEvent that = (RestSBServerSentEvent) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(comment, that.comment) &&
                Objects.equals(data, that.data) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, comment, data, name);
    }
}
