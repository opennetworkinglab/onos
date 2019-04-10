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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;
import org.onosproject.grpc.api.GrpcClientController;

/**
 * Controller of P4Runtime clients.
 */
@Beta
public interface P4RuntimeController
        extends GrpcClientController<P4RuntimeClient>,
                ListenerService<P4RuntimeEvent, P4RuntimeEventListener> {

}
