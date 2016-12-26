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
package org.onosproject.lisp.ctl;

import org.onosproject.lisp.msg.protocols.LispMessage;

/**
 * Test adapter for the LISP router agent interface.
 */
public class LispRouterAgentAdapter implements LispRouterAgent {
    @Override
    public boolean addConnectedRouter(LispRouterId routerId, LispRouter router) {
        return false;
    }

    @Override
    public void removeConnectedRouter(LispRouterId routerId) {

    }

    @Override
    public void processUpstreamMessage(LispRouterId routerId, LispMessage message) {

    }

    @Override
    public void processDownstreamMessage(LispRouterId routerId, LispMessage message) {

    }
}
