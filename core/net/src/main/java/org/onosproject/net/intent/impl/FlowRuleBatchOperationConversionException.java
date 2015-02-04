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
package org.onosproject.net.intent.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.intent.IntentException;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Reconsider error handling and intent exception design. Otherwise, write Javadoc.
public class FlowRuleBatchOperationConversionException extends IntentException {

    private final List<FlowRuleBatchOperation> converted;

    public FlowRuleBatchOperationConversionException(List<FlowRuleBatchOperation> converted, Throwable cause) {
        super("exception occurred during IntentInstaller.install()", cause);
        this.converted = ImmutableList.copyOf((checkNotNull(converted)));
    }

    public List<FlowRuleBatchOperation> converted() {
        return converted;
    }
}
