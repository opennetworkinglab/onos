/*
 * Copyright 2016-present Open Networking Foundation
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

/**
 * Test adapter for the LISP controller interface.
 */
public class LispControllerAdapter implements LispController {
    @Override
    public Iterable<LispRouter> getRouters() {
        return null;
    }

    @Override
    public Iterable<LispRouter> getSubscribedRouters() {
        return null;
    }

    @Override
    public LispRouter connectRouter(LispRouterId routerId) {
        return null;
    }

    @Override
    public void disconnectRouter(LispRouterId routerId, boolean remove) {

    }

    @Override
    public LispRouter getRouter(LispRouterId routerId) {
        return null;
    }

    @Override
    public void addRouterListener(LispRouterListener listener) {

    }

    @Override
    public void removeRouterListener(LispRouterListener listener) {

    }

    @Override
    public void addMessageListener(LispMessageListener listener) {

    }

    @Override
    public void removeMessageListener(LispMessageListener listener) {

    }
}
