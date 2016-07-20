/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.config;

/**
 * An interface signifying a class that implements network configuration
 * information from multiple sources. There is a natural ordering to the
 * precedence of information, depending on its source:
 * <ol>
 * <li>Intents (from applications), which override</li>
 * <li>Configs (from the network configuration subsystem), which override</li>
 * <li>Descriptions (from southbound)</li>
 * </ol>
 * i.e., for a field representing the same attribute, the value from a Config
 * entity will be used over that from the Description.
 */
public interface ConfigOperator {
}
