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

package org.onosproject.p4runtime.ctl.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.onosproject.net.pi.model.PiPipeconf;
import org.slf4j.Logger;
import p4.config.v1.P4InfoOuterClass.P4Info;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility class to deal with pipeconfs in the context of P4Runtime.
 */
public final class PipeconfHelper {

    private static final int P4INFO_BROWSER_EXPIRE_TIME_IN_MIN = 10;
    private static final Logger log = getLogger(PipeconfHelper.class);

    private static final Cache<Long, P4InfoBrowser> BROWSERS = CacheBuilder.newBuilder()
            .expireAfterAccess(P4INFO_BROWSER_EXPIRE_TIME_IN_MIN, TimeUnit.MINUTES)
            .build();
    private static final Map<Long, P4Info> P4INFOS = Maps.newConcurrentMap();

    private PipeconfHelper() {
        // hide.
    }

    /**
     * Extracts and returns a P4Info protobuf message from the given pipeconf. If the pipeconf does not define any
     * extension of type {@link PiPipeconf.ExtensionType#P4_INFO_TEXT}, returns null;
     *
     * @param pipeconf pipeconf
     * @return P4Info or null
     */
    public static P4Info getP4Info(PiPipeconf pipeconf) {
        return P4INFOS.computeIfAbsent(pipeconf.fingerprint(), piPipeconfId -> {
            if (!pipeconf.extension(P4_INFO_TEXT).isPresent()) {
                log.warn("Missing P4Info extension in pipeconf {}", pipeconf.id());
                return null;
            }

            InputStream p4InfoStream = pipeconf.extension(P4_INFO_TEXT).get();
            P4Info.Builder p4iInfoBuilder = P4Info.newBuilder();
            try {
                TextFormat.getParser().merge(new InputStreamReader(p4InfoStream), ExtensionRegistry.getEmptyRegistry(),
                                             p4iInfoBuilder);
            } catch (IOException ex) {
                log.warn("Unable to parse P4Info of pipeconf {}: {}", pipeconf.id(), ex.getMessage());
                return null;
            }

            return p4iInfoBuilder.build();
        });
    }

    /**
     * Returns a P4Info browser for the given pipeconf. If the pipeconf does not define any extension of type
     * {@link PiPipeconf.ExtensionType#P4_INFO_TEXT}, returns null;
     *
     * @param pipeconf pipeconf
     * @return P4Info browser or null
     */
    public static P4InfoBrowser getP4InfoBrowser(PiPipeconf pipeconf) {
        try {
            return BROWSERS.get(pipeconf.fingerprint(), () -> {
                P4Info p4info = PipeconfHelper.getP4Info(pipeconf);
                if (p4info == null) {
                    return null;
                } else {
                    return new P4InfoBrowser(p4info);
                }
            });
        } catch (ExecutionException e) {
            log.error("Exception while accessing the P4InfoBrowser cache", e);
            return null;
        }
    }
}
