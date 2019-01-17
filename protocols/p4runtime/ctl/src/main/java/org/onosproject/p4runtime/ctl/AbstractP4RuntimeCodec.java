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

package org.onosproject.p4runtime.ctl;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiEntity;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of a codec that translates PI entities into P4Runtime
 * protobuf messages and vice versa.
 *
 * @param <P> PI entity class
 * @param <M> P4Runtime protobuf message class
 */
abstract class AbstractP4RuntimeCodec<P extends PiEntity, M extends Message> {

    protected final Logger log = getLogger(this.getClass());

    protected abstract M encode(P piEntity, PiPipeconf pipeconf,
                                P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException;

    protected abstract P decode(M message, PiPipeconf pipeconf,
                                P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException;

    /**
     * Returns a P4Runtime protobuf message that is equivalent to the given PI
     * entity for the given pipeconf.
     *
     * @param piEntity PI entity instance
     * @param pipeconf pipeconf
     * @return P4Runtime protobuf message
     * @throws CodecException if the given PI entity cannot be encoded (see
     *                        exception message)
     */
    public M encode(P piEntity, PiPipeconf pipeconf)
            throws CodecException {
        try {
            return encode(piEntity, pipeconf, browserOrFail(pipeconf));
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }

    /**
     * Returns a PI entity instance that is equivalent to the P4Runtime protobuf
     * message for the given pipeconf.
     *
     * @param message  P4Runtime protobuf message
     * @param pipeconf pipeconf pipeconf
     * @return PI entity instance
     * @throws CodecException if the given protobuf message cannot be decoded
     *                        (see exception message)
     */
    public P decode(M message, PiPipeconf pipeconf)
            throws CodecException {
        try {
            return decode(message, pipeconf, browserOrFail(pipeconf));
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }

    /**
     * Same as {@link #encode(PiEntity, PiPipeconf)} but returns null in case of
     * exceptions, while the error message is logged.
     *
     * @param piEntity PI entity instance
     * @param pipeconf pipeconf
     * @return P4Runtime protobuf message
     */
    public M encodeOrNull(P piEntity, PiPipeconf pipeconf) {
        try {
            return encode(piEntity, pipeconf);
        } catch (CodecException e) {
            log.error("Unable to encode {}: {} [{}]",
                      piEntity.getClass().getSimpleName(),
                      e.getMessage(), piEntity.toString());
            return null;
        }
    }

    /**
     * Same as {@link #decode(Message, PiPipeconf)} but returns null in case of
     * exceptions, while the error message is logged.
     *
     * @param message  P4Runtime protobuf message
     * @param pipeconf pipeconf pipeconf
     * @return PI entity instance
     */
    public P decodeOrNull(M message, PiPipeconf pipeconf) {
        try {
            return decode(message, pipeconf);
        } catch (CodecException e) {
            log.error("Unable to decode {}: {} [{}]",
                      message.getClass().getSimpleName(),
                      e.getMessage(), TextFormat.shortDebugString(message));
            return null;
        }
    }

    /**
     * Encodes the given list of PI entities, skipping those that cannot be
     * encoded, in which case an error message is logged. For this reason, the
     * returned list might have different size than the returned one.
     *
     * @param piEntities list of PI entities
     * @param pipeconf   pipeconf
     * @return list of P4Runtime protobuf messages
     */
    public List<M> encodeAll(List<P> piEntities, PiPipeconf pipeconf) {
        checkNotNull(piEntities);
        return piEntities.stream()
                .map(p -> encodeOrNull(p, pipeconf))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Decodes the given list of P4Runtime protobuf messages, skipping those
     * that cannot be decoded, on which case an error message is logged. For
     * this reason, the returned list might have different size than the
     * returned one.
     *
     * @param messages list of protobuf messages
     * @param pipeconf pipeconf
     * @return list of PI entities
     */
    public List<P> decodeAll(List<M> messages, PiPipeconf pipeconf) {
        checkNotNull(messages);
        return messages.stream()
                .map(m -> decodeOrNull(m, pipeconf))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Same as {@link #encodeAll(List, PiPipeconf)} but throws an exception if
     * one or more of the given PI entities cannot be encoded. The returned list
     * is guaranteed to have same size and order as the given one.
     *
     * @param piEntities list of PI entities
     * @param pipeconf   pipeconf
     * @return list of protobuf messages
     * @throws CodecException if one or more of the given PI entities cannot be
     *                        encoded
     */
    public List<M> encodeAllOrFail(List<P> piEntities, PiPipeconf pipeconf)
            throws CodecException {
        final List<M> messages = encodeAll(piEntities, pipeconf);
        if (piEntities.size() != messages.size()) {
            throw new CodecException(format(
                    "Unable to encode %d entities of %d given " +
                            "(see previous logs for details)",
                    piEntities.size() - messages.size(), piEntities.size()));
        }
        return messages;
    }

    /**
     * Same as {@link #decodeAll(List, PiPipeconf)} but throws an exception if
     * one or more of the given protobuf messages cannot be decoded. The
     * returned list is guaranteed to have same size and order as the given
     * one.
     *
     * @param messages list of protobuf messages
     * @param pipeconf pipeconf
     * @return list of PI entities
     * @throws CodecException if one or more of the given protobuf messages
     *                        cannot be decoded
     */
    public List<P> decodeAllOrFail(List<M> messages, PiPipeconf pipeconf)
            throws CodecException {
        final List<P> piEntities = decodeAll(messages, pipeconf);
        if (messages.size() != piEntities.size()) {
            throw new CodecException(format(
                    "Unable to decode %d messages of %d given " +
                            "(see previous logs for details)",
                    messages.size() - piEntities.size(), messages.size()));
        }
        return piEntities;
    }

    private P4InfoBrowser browserOrFail(PiPipeconf pipeconf) throws CodecException {
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            throw new CodecException(format(
                    "Unable to get P4InfoBrowser for pipeconf %s", pipeconf.id()));
        }
        return browser;
    }
}
