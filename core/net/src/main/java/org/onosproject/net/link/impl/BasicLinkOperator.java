/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.link.impl;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.config.ConfigOperator;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Link;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.slf4j.Logger;

/**
 * Implementations of merge policies for various sources of link configuration
 * information. This includes applications, provides, and network configurations.
 */
public final class BasicLinkOperator implements ConfigOperator {

    private static final long DEF_BANDWIDTH = -1L;
    private static final double DEF_METRIC = -1;
    private static final Duration DEF_DURATION = Duration.ofNanos(-1L);
    private static final Logger log = getLogger(BasicLinkOperator.class);

    private BasicLinkOperator() {
    }

    /**
     * Generates a LinkDescription containing fields from a LinkDescription and
     * a LinkConfig.
     *
     * @param cfg the link config entity from network config
     * @param descr a LinkDescription
     * @return LinkDescription based on both sources
     */
    public static LinkDescription combine(BasicLinkConfig cfg, LinkDescription descr) {
        if (cfg == null) {
            return descr;
        }

        Link.Type type = descr.type();
        if (cfg.isTypeConfigured()) {
            if (cfg.type() != type) {
                type = cfg.type();
            }
        }

        SparseAnnotations sa = combine(cfg, descr.annotations());
        return new DefaultLinkDescription(descr.src(), descr.dst(), type, sa);
    }

    /**
     * Generates an annotation from an existing annotation and LinkConfig.
     *
     * @param cfg the link config entity from network config
     * @param an the annotation
     * @return annotation combining both sources
     */
    public static SparseAnnotations combine(BasicLinkConfig cfg, SparseAnnotations an) {
        DefaultAnnotations.Builder b = DefaultAnnotations.builder();
        b.putAll(an);
        if (cfg.metric() != DEF_METRIC) {
            b.set(AnnotationKeys.METRIC, String.valueOf(cfg.metric()));
        }
        if (!cfg.latency().equals(DEF_DURATION)) {
            //Convert the latency from Duration to long,
            //so that it's computable in the latencyConstraint.
            b.set(AnnotationKeys.LATENCY, String.valueOf(cfg.latency().toNanos()));
        }
        if (cfg.bandwidth() != DEF_BANDWIDTH) {
            b.set(AnnotationKeys.BANDWIDTH, String.valueOf(cfg.bandwidth()));
        }
        if (cfg.isDurable() != null) {
            b.set(AnnotationKeys.DURABLE, String.valueOf(cfg.isDurable()));
        }
        return b.build();
    }

    /**
     * Generates a link description from a link description entity. The endpoints
     * must be specified to indicate directionality.
     *
     * @param src the source ConnectPoint
     * @param dst the destination ConnectPoint
     * @param link the link config entity
     * @return a linkDescription based on the config
     */
    public static LinkDescription descriptionOf(
                ConnectPoint src, ConnectPoint dst, Link link) {
        checkNotNull(src, "Must supply a source endpoint");
        checkNotNull(dst, "Must supply a destination endpoint");
        checkNotNull(link, "Must supply a link");
        return new DefaultLinkDescription(
                src, dst, link.type(),
                link.isExpected(),
                (SparseAnnotations) link.annotations());
    }

    /**
     * Generates a link description from a link config entity. This is for
     * links that cannot be discovered and has to be injected. The endpoints
     * must be specified to indicate directionality.
     *
     * @param src the source ConnectPoint
     * @param dst the destination ConnectPoint
     * @param link the link config entity
     * @return a linkDescription based on the config
     */
    public static LinkDescription descriptionOf(
                ConnectPoint src, ConnectPoint dst, BasicLinkConfig link) {
        checkNotNull(src, "Must supply a source endpoint");
        checkNotNull(dst, "Must supply a destination endpoint");
        checkNotNull(link, "Must supply a link config");
        // Only allowed link is expected link
        boolean expected = link.isAllowed();
        return new DefaultLinkDescription(
                src, dst, link.type(),
                expected,
                combine(link, DefaultAnnotations.EMPTY));
    }
}
