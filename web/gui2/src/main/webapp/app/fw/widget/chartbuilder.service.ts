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
import { Injectable } from '@angular/core';
import { FnService } from '../util/fn.service';
import { LoadingService } from '../layer/loading.service';
import { LogService } from '../../log.service';
import { WebSocketService } from '../remote/websocket.service';

/**
 * ONOS GUI -- Widget -- Chart Service
 */
@Injectable()
export class ChartBuilderService {

    constructor(
        private fs: FnService,
        private ls: LoadingService,
        private log: LogService,
        private wss: WebSocketService
    ) {
        this.log.debug('ChartBuilderService constructed');
    }

}
