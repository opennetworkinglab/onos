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
package org.onosproject.netconf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBElement;

import org.onosproject.netconf.rpc.ErrorInfoType;
import org.onosproject.netconf.rpc.ErrorSeverity;
import org.onosproject.netconf.rpc.ErrorTag;
import org.onosproject.netconf.rpc.ErrorType;
import org.onosproject.netconf.rpc.RpcErrorType;
import org.onosproject.netconf.rpc.RpcErrorType.ErrorMessage;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Wrapper around {@link RpcErrorType} for ease of handling and logging.
 *
 * @see NetconfRpcParserUtil
 */
@Beta
public class NetconfRpcError {

    /**
     * Protocol mandated error-info: {@value}.
     */
    public static final String BAD_ATTRIBUTE = "bad-attribute";
    /**
     * Protocol mandated error-info: {@value}.
     */
    public static final String BAD_ELEMENT = "bad-element";
    /**
     * Protocol mandated error-info: {@value}.
     */
    public static final String OK_ELEMENT = "ok-element";
    /**
     * Protocol mandated error-info: {@value}.
     */
    public static final String ERR_ELEMENT = "err-element";
    /**
     * Protocol mandated error-info: {@value}.
     */
    public static final String NOOP_ELEMENT = "noop-element";
    /**
     * Protocol mandated error-info: {@value}.
     */
    public static final String BAD_NAMESPACE = "bad-namespace";

    private final RpcErrorType msg;


    public static final NetconfRpcError wrap(RpcErrorType msg) {
        return new NetconfRpcError(msg);
    }

    protected NetconfRpcError(RpcErrorType msg) {
        this.msg = checkNotNull(msg);
    }

    /**
     * Returns conceptual layer that the error occurred.
     *
     * @return the type
     */
    public ErrorType type() {
        return msg.getErrorType();
    }

    /**
     * Returns a tag identifying the error condition.
     *
     * @return the tag
     */
    public ErrorTag tag() {
        return msg.getErrorTag();
    }

    /**
     * Returns error severity.
     *
     * @return severity
     */
    public ErrorSeverity severity() {
        return msg.getErrorSeverity();
    }

    /**
     * Returns a string identifying the data-model-specific
     * or implementation-specific error condition, if one exists.
     *
     * @return app tag
     */
    public Optional<String> appTag() {
        return Optional.ofNullable(msg.getErrorAppTag());
    }

    /**
     * Returns absolute XPath expression identifying the element path
     * to the node that is associated with the error being reported.
     *
     * @return XPath expression
     */
    public Optional<String> path() {
        return Optional.ofNullable(msg.getErrorPath());
    }

    /**
     * Returns human readable error message.
     *
     * @return message
     */
    //@Nonnull
    public String message() {
        return Optional.ofNullable(msg.getErrorMessage())
                    .map(ErrorMessage::getValue)
                    .orElse("");
    }

    /**
     * Returns session-id in error-info if any.
     *
     * @return session-id
     */
    public Optional<Long> infoSessionId() {
        return Optional.ofNullable(msg.getErrorInfo())
                    .map(ErrorInfoType::getSessionId);
    }

    /**
     * Returns protocol-mandated contents if any.
     *
     * @return Map containing protocol-mandated contents
     * <p>
     * Possible Map keys:
     * {@link #BAD_ATTRIBUTE},
     * {@link #BAD_ELEMENT},
     * {@link #OK_ELEMENT},
     * {@link #ERR_ELEMENT},
     * {@link #NOOP_ELEMENT},
     * {@link #BAD_NAMESPACE}
     */
    @Beta
    public Map<String, String> info() {
        return Optional.ofNullable(msg.getErrorInfo())
                .map(ErrorInfoType::getBadAttributeAndBadElementAndOkElement)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .collect(Collectors.<JAXBElement<? extends Serializable>,
                                     String, String, Map<String, String>>
                                    toMap(elm -> elm.getName().getLocalPart(),
                                          elm -> String.valueOf(elm.getValue()),
                                          (v1, v2) -> v1,
                                          LinkedHashMap::new));
    }

    /**
     * Returns any other elements if present.
     *
     * @return any other elements
     */
    @Beta
    public List<Object> infoAny() {
        return Optional.ofNullable(msg.getErrorInfo())
                  .map(ErrorInfoType::getAny)
                  .orElse(ImmutableList.of());
    }

    /**
     * Returns protocol- or data-model-specific error content.
     *
     * @return error info
     */
    @Beta
    public Optional<ErrorInfoType> rawInfo() {
        return Optional.ofNullable(msg.getErrorInfo());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type())
                .add("tag", tag())
                .add("severity", severity())
                .add("appTag", appTag().orElse(null))
                .add("path", path().orElse(null))
                .add("message", Strings.emptyToNull(message()))
                .add("info-session-id", infoSessionId().orElse(null))
                .add("info", info())
                .omitNullValues()
                .toString();
    }
}
