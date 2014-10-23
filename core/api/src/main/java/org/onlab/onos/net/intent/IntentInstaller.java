/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.net.intent;

import java.util.List;

import org.onlab.onos.net.flow.FlowRuleBatchOperation;

/**
 * Abstraction of entity capable of installing intents to the environment.
 */
public interface IntentInstaller<T extends Intent> {
    /**
     * Installs the specified intent to the environment.
     *
     * @param intent intent to be installed
     * @throws IntentException if issues are encountered while installing the intent
     */
    List<FlowRuleBatchOperation> install(T intent);

    /**
     * Uninstalls the specified intent from the environment.
     *
     * @param intent intent to be uninstalled
     * @throws IntentException if issues are encountered while uninstalling the intent
     */
    List<FlowRuleBatchOperation> uninstall(T intent);
}
