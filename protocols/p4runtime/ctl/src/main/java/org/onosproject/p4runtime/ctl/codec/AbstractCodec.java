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

package org.onosproject.p4runtime.ctl.codec;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import org.onosproject.p4runtime.ctl.utils.PipeconfHelper;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of a general codec that translates pipeconf-related
 * objects into protobuf messages and vice versa.
 *
 * @param <P> object
 * @param <M> protobuf message class
 * @param <X> metadata class
 */
abstract class AbstractCodec<P, M extends Message, X> {

    protected final Logger log = getLogger(this.getClass());

    protected abstract M encode(P object, X metadata, PiPipeconf pipeconf,
                                P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException;

    protected abstract P decode(M message, X metadata, PiPipeconf pipeconf,
                                P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException;

    /**
     * Returns a protobuf message that is equivalent to the given object for the
     * given metadata and pipeconf.
     *
     * @param object   object
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return protobuf message
     * @throws CodecException if the given object cannot be encoded (see
     *                        exception message)
     */
    public M encode(P object, X metadata, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(object);
        try {
            return encode(object, metadata, pipeconf, browserOrFail(pipeconf));
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }

    /**
     * Returns a object that is equivalent to the protobuf message for the given
     * metadata and pipeconf.
     *
     * @param message  protobuf message
     * @param metadata metadata
     * @param pipeconf pipeconf pipeconf
     * @return object
     * @throws CodecException if the given protobuf message cannot be decoded
     *                        (see exception message)
     */
    public P decode(M message, X metadata, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(message);
        try {
            return decode(message, metadata, pipeconf, browserOrFail(pipeconf));
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }

    /**
     * Same as {@link #encode(Object, Object, PiPipeconf)} but returns null in
     * case of exceptions, while the error message is logged.
     *
     * @param object   object
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return protobuf message
     */
    private M encodeOrNull(P object, X metadata, PiPipeconf pipeconf) {
        checkNotNull(object);
        try {
            return encode(object, metadata, pipeconf);
        } catch (CodecException e) {
            log.error("Unable to encode {}: {} [{}]",
                      object.getClass().getSimpleName(),
                      e.getMessage(), object.toString());
            return null;
        }
    }

    /**
     * Same as {@link #decode(Message, Object, PiPipeconf)} but returns null in
     * case of exceptions, while the error message is logged.
     *
     * @param message  protobuf message
     * @param metadata metadata
     * @param pipeconf pipeconf pipeconf
     * @return object
     */
    private P decodeOrNull(M message, X metadata, PiPipeconf pipeconf) {
        checkNotNull(message);
        try {
            return decode(message, metadata, pipeconf);
        } catch (CodecException e) {
            log.error("Unable to decode {}: {} [{}]",
                      message.getClass().getSimpleName(),
                      e.getMessage(), TextFormat.shortDebugString(message));
            return null;
        }
    }

    /**
     * Encodes the given list of objects, skipping those that cannot be encoded,
     * in which case an error message is logged. For this reason, the returned
     * list might have different size than the returned one.
     *
     * @param objects  list of objects
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return list of protobuf messages
     */
    private List<M> encodeAllSkipException(
            Collection<P> objects, X metadata, PiPipeconf pipeconf) {
        checkNotNull(objects);
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }
        return objects.stream()
                .map(p -> encodeOrNull(p, metadata, pipeconf))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Decodes the given list of protobuf messages, skipping those that cannot
     * be decoded, on which case an error message is logged. For this reason,
     * the returned list might have different size than the returned one.
     *
     * @param messages list of protobuf messages
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return list of objects
     */
    private List<P> decodeAllSkipException(
            Collection<M> messages, X metadata, PiPipeconf pipeconf) {
        checkNotNull(messages);
        if (messages.isEmpty()) {
            return ImmutableList.of();
        }
        return messages.stream()
                .map(m -> decodeOrNull(m, metadata, pipeconf))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Encodes the given collection of objects. Throws an exception if one or
     * more of the given objects cannot be encoded. The returned list is
     * guaranteed to have same size and order as the given one.
     *
     * @param objects  list of objects
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return list of protobuf messages
     * @throws CodecException if one or more of the given objects cannot be
     *                        encoded
     */
    List<M> encodeAll(Collection<P> objects, X metadata, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(objects);
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }
        final List<M> messages = encodeAllSkipException(objects, metadata, pipeconf);
        if (objects.size() != messages.size()) {
            throw new CodecException(format(
                    "Unable to encode %d entities of %d given " +
                            "(see previous logs for details)",
                    objects.size() - messages.size(), objects.size()));
        }
        return messages;
    }

    /**
     * Decodes the given collection of protobuf messages. Throws an exception if
     * one or more of the given protobuf messages cannot be decoded. The
     * returned list is guaranteed to have same size and order as the given
     * one.
     *
     * @param messages list of protobuf messages
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return list of objects
     * @throws CodecException if one or more of the given protobuf messages
     *                        cannot be decoded
     */
    List<P> decodeAll(Collection<M> messages, X metadata, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(messages);
        if (messages.isEmpty()) {
            return ImmutableList.of();
        }
        final List<P> objects = decodeAllSkipException(messages, metadata, pipeconf);
        if (messages.size() != objects.size()) {
            throw new CodecException(format(
                    "Unable to decode %d messages of %d given " +
                            "(see previous logs for details)",
                    messages.size() - objects.size(), messages.size()));
        }
        return objects;
    }

    /**
     * Returns a P4Info browser for the given pipeconf or throws a
     * CodecException if not possible.
     *
     * @param pipeconf pipeconf
     * @return P4Info browser
     * @throws CodecException if a P4Info browser cannot be obtained
     */
    P4InfoBrowser browserOrFail(PiPipeconf pipeconf) throws CodecException {
        checkNotNull(pipeconf);
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            throw new CodecException(format(
                    "Unable to get P4InfoBrowser for pipeconf %s", pipeconf.id()));
        }
        return browser;
    }
}
