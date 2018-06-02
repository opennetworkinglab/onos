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
import { Component, OnInit } from '@angular/core';
import { DetailsPanelService } from '../../fw/layer/detailspanel.service';
import { FnService } from '../../fw/util/fn.service';
import { IconService } from '../../fw/svg/icon.service';
import { KeyService } from '../../fw/util/key.service';
import { LoadingService } from '../../fw/layer/loading.service';
import { LogService } from '../../log.service';
import { MastService } from '../../fw/mast/mast.service';
import { NavService } from '../../fw/nav/nav.service';
import { PanelService } from '../../fw/layer/panel.service';
import { TableBuilderService } from '../../fw/widget/tablebuilder.service';
import { TableDetailService } from '../../fw/widget/tabledetail.service';
import { WebSocketService } from '../../fw/remote/websocket.service';

/**
 * ONOS GUI -- Device View Component
 */
@Component({
  selector: 'onos-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.css']
})
export class DeviceComponent implements OnInit {

    constructor(
        private dps: DetailsPanelService,
        private fs: FnService,
        private is: IconService,
        private ks: KeyService,
        private log: LogService,
        private mast: MastService,
        private nav: NavService,
        private ps: PanelService,
        private tbs: TableBuilderService,
        private tds: TableDetailService,
        private wss: WebSocketService,
        private ls: LoadingService, // TODO: Remove - already added through tbs
        private window: Window
    ) {
        this.log.debug('DeviceComponent constructed');
    }

    ngOnInit() {
        this.log.debug('DeviceComponent initialized');
        // TODO: Remove this - it's only for demo purposes
//        this.ls.startAnim();
    }

}
