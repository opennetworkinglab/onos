/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pcep.controller;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

/**
 * Unique Srp Id generator for pcep messages.
 */
public final class SrpIdGenerators {

    private static final Logger log = getLogger(SrpIdGenerators.class);
    private static final AtomicInteger SRP_ID_GEN = new AtomicInteger();
    private static final int MAX_SRP_ID = 0x7FFFFFFF;
    private static int srpId;

    /**
     * Default constructor.
     */
    private SrpIdGenerators() {
    }

    /**
     * Get the next srp id.
     *
     * @return srp id
     */
    public static int create() {
        do {
            if (srpId >= MAX_SRP_ID) {
                if (SRP_ID_GEN.get() >= MAX_SRP_ID) {
                    SRP_ID_GEN.set(0);
                }
            }
            srpId = SRP_ID_GEN.incrementAndGet();
        } while (srpId > MAX_SRP_ID);
        return srpId;
    }
}
