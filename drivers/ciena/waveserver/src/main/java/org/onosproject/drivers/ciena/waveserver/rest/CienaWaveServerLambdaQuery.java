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
package org.onosproject.drivers.ciena.waveserver.rest;

import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;


import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * Ciena WaveServer Lambda query.
 * 88 50GHz flex grid channels with 6.25 slot width, starting from 0 to 87.
 */
public class CienaWaveServerLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        // 88 channels of 50 GHz with 6.25 GHz slothWidth
        int slots = (int) (ChannelSpacing.CHL_50GHZ.frequency().asHz() /
                ChannelSpacing.CHL_6P25GHZ.frequency().asHz());
        int channels = 88;
        // total lambdas are equal to: channels * slots
        return IntStream.rangeClosed(0, channels * slots)
                .mapToObj(OchSignal::newFlexGridSlot)
                .collect(ImmutableSet.toImmutableSet());
    }

}


