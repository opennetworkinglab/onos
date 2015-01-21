/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.flowext;

import org.onosproject.net.flow.FlowRuleBatchEvent;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 * The monitor module of the router.
 * <p>
 * The monitor module of router.
 * </p>
 */
public interface FlowRuleExtRouterListener {

    /**
     * Notify monitor the router has down its work.
     *
     * @param event the event to notify
     */
    void notify(FlowRuleBatchEvent event);
}
