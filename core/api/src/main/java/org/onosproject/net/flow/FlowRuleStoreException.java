/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.flow;

/**
 * Top level exception for FlowRuleStore failures.
 */
@SuppressWarnings("serial")
public class FlowRuleStoreException extends RuntimeException {
    public FlowRuleStoreException() {
    }

    public FlowRuleStoreException(String message) {
        super(message);
    }

    public FlowRuleStoreException(Throwable t) {
        super(t);
    }

    /**
     * Flowrule store operation timeout.
     */
    public static class Timeout extends FlowRuleStoreException {
    }

    /**
     * Flowrule store operation interrupted.
     */
    public static class Interrupted extends FlowRuleStoreException {
    }
}
