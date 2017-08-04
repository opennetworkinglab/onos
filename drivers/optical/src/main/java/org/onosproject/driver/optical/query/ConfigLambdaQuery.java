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
package org.onosproject.driver.optical.query;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.onosproject.driver.optical.config.LambdaConfig;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

/**
 * {@link LambdaQuery} which responds based on {@link LambdaConfig}.
 */
public class ConfigLambdaQuery
        extends AbstractHandlerBehaviour
        implements LambdaQuery {

    private static final Logger log = getLogger(ConfigLambdaQuery.class);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {

        NetworkConfigService netcfg = handler().get(NetworkConfigService.class);
        ConnectPoint cp = new ConnectPoint(data().deviceId(), port);
        LambdaConfig cfg = netcfg.getConfig(cp, LambdaConfig.class);
        if (cfg == null) {
            return ImmutableSet.of();
        }

        GridType type = cfg.gridType();
        Optional<ChannelSpacing> dwdmSpacing = cfg.dwdmSpacing();
        int start = cfg.slotStart();
        int step = cfg.slotStep();
        int end = cfg.slotEnd();

        Set<OchSignal> lambdas = new LinkedHashSet<>();
        for (int i = start; i <= end; i += step) {
            switch (type) {
            case DWDM:
                lambdas.add(OchSignal.newDwdmSlot(dwdmSpacing.get(), i));
                break;

            case FLEX:
                lambdas.add(OchSignal.newFlexGridSlot(i));
                break;

            case CWDM:
            default:
                log.warn("Unsupported grid type: {}", type);
                break;
            }
        }
        return lambdas;
    }

}
