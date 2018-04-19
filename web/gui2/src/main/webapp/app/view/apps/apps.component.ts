/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, OnInit } from '@angular/core';
import { DialogService } from '../../fw/layer/dialog.service';
import { FnService } from '../../fw/util/fn.service';
import { IconService } from '../../fw/svg/icon.service';
import { KeyService } from '../../fw/util/key.service';
import { LionService } from '../../fw/util/lion.service';
import { LogService } from '../../log.service';
import { PanelService } from '../../fw/layer/panel.service';
import { TableBuilderService } from '../../fw/widget/tablebuilder.service';
import { UrlFnService } from '../../fw/remote/urlfn.service';
import { WebSocketService } from '../../fw/remote/websocket.service';

/**
 * ONOS GUI -- Apps View Component
 */
@Component({
  selector: 'onos-apps',
  templateUrl: './apps.component.html',
  styleUrls: ['./apps.component.css']
})
export class AppsComponent implements OnInit {

    constructor(
        private fs: FnService,
        private ds: DialogService,
        private is: IconService,
        private ks: KeyService,
        private ls: LionService,
        private log: LogService,
        private ps: PanelService,
        private tbs: TableBuilderService,
        private ufs: UrlFnService,
        private wss: WebSocketService,
        private window: Window
    ) {
        this.log.debug('AppsComponent constructed');
    }

    ngOnInit() {
        this.log.debug('AppsComponent initialized');
    }

}
