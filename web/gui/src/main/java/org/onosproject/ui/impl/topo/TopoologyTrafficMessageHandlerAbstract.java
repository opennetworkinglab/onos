/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.ui.impl.topo;

import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.Highlights;

/**
 * Base superclass for traffic message handler (both 'classic' and 'topo2' versions).
 */
public abstract class TopoologyTrafficMessageHandlerAbstract extends UiMessageHandler {
    public abstract void sendHighlights(Highlights highlights);
}
