/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.d.config.sync.operation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.d.config.sync.operation.SetRequest.Change.Operation;
import org.onosproject.yang.model.ResourceId;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;


@Beta
public final class SetResponse {

    // partially borrowed from io.grpc.Status.Code,
    // might want to borrow all of them
    public enum Code {
        OK,
        CANCELLED,

        UNKNOWN,

        INVALID_ARGUMENT,

        NOT_FOUND,
        ALREADY_EXISTS,

        FAILED_PRECONDITION,
        ABORTED,
        UNAVAILABLE,
    }

    private final Collection<Pair<Operation, ResourceId>> subjects;

    private final SetResponse.Code code;

    // human readable error message for logging purpose
    private final String message;

    SetResponse(Collection<Pair<Operation, ResourceId>> subjects,
                SetResponse.Code code,
                String message) {
        this.subjects = ImmutableList.copyOf(subjects);
        this.code = checkNotNull(code);
        this.message = checkNotNull(message);
    }

    public Collection<Pair<Operation, ResourceId>> subjects() {
        return subjects;
    }

    public Code code() {
        return code;
    }

    public String message() {
        return message;
    }


    /**
     * Creates SetResponse instance from request.
     *
     * @param request original request this response corresponds to
     * @param code response status code
     * @param message human readable error message for logging purpose.
     *        can be left empty string on OK response.
     * @return SetResponse instance
     */
    public static SetResponse response(SetRequest request,
                                       Code code,
                                       String message) {
        return new SetResponse(request.subjects(), code, checkNotNull(message));
    }

    /**
     * Creates successful SetResponce instance from request.
     *
     * @param request original request this response corresponds to
     * @return SetResponse instance
     */
    public static SetResponse ok(SetRequest request) {
        return new SetResponse(request.subjects(), Code.OK, "");
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjects, code, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SetResponse) {
            SetResponse that = (SetResponse) obj;
            return Objects.equals(this.subjects, that.subjects) &&
                    Objects.equals(this.code, that.code) &&
                    Objects.equals(this.message, that.message);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("code", code)
                .add("subjects", subjects)
                .add("message", message)
                .toString();
    }



}