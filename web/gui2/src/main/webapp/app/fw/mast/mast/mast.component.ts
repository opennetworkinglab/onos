/*
 * Copyright 2014-present Open Networking Foundation
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
import { Component, OnInit } from '@angular/core';
import { DialogService } from '../../layer/dialog.service';
import { LionService } from '../../util/lion.service';
import { LogService } from '../../../log.service';
import { NavService } from '../../nav/nav.service';
import { WebSocketService } from '../../remote/websocket.service';

/**
 * ONOS GUI -- Masthead Component
 */
@Component({
  selector: 'onos-mast',
  templateUrl: './mast.component.html',
  styleUrls: ['./mast.component.css', './mast.theme.css']
})
export class MastComponent implements OnInit {
    public username;

    constructor(
        private ds: DialogService,
        private ls: LionService,
        private log: LogService,
        public ns: NavService,
        private wss: WebSocketService
    ) {
        this.log.debug('MastComponent constructed');

    }

    ngOnInit() {
        // onosUser is a global set via the index.html generated source
        // TODO: Fix onosuser below to get it from index.html like before
        // TODO: Fix the lionService
        this.username = 'onosUser'; // || this.getLion('unknown_user');

        this.log.debug('MastComponent initialized');
    }



    /* In the case of Masthead, we cannot cache the lion bundle, because we
     * call too early (before the lion data is uploaded from the server).
     * So we'll dig into the lion service for each request...
     */
    getLion(x: string): string {
      // lion is a function that takes a string and returns a string
      const lion: (string) => string  = this.ls.bundle('core.fw.Mast');
      return lion(x);
    }

}
