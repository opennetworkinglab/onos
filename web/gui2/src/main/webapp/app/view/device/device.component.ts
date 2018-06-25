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
import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { FnService } from '../../fw/util/fn.service';
import { IconService } from '../../fw/svg/icon.service';
import { KeyService } from '../../fw/util/key.service';
import { LoadingService } from '../../fw/layer/loading.service';
import { LogService } from '../../log.service';
import { MastService } from '../../fw/mast/mast.service';
import { NavService } from '../../fw/nav/nav.service';
import { TableBaseImpl, TableResponse } from '../../fw/widget/table.base';
import { WebSocketService } from '../../fw/remote/websocket.service';

/**
 * Model of the response from WebSocket
 */
interface DeviceTableResponse extends TableResponse {
    devices: Device[];
}

/**
 * Model of the devices returned from the WebSocket
 */
interface Device {
    available: boolean;
    chassisid: string;
    hw: string;
    id: string;
    masterid: string;
    mfr: string;
    name: string;
    num_ports: number;
    protocol: string;
    serial: string;
    sw: string;
    _iconid_available: string;
    _iconid_type: string;
}


/**
 * ONOS GUI -- Device View Component
 */
@Component({
  selector: 'onos-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.css', './device.theme.css', '../../fw/widget/table.css', '../../fw/widget/table.theme.css']
})
export class DeviceComponent extends TableBaseImpl implements OnInit, OnDestroy {

    // TODO: Update for LION
    flowTip = 'Show flow view for selected device';
    portTip = 'Show port view for selected device';
    groupTip = 'Show group view for selected device';
    meterTip = 'Show meter view for selected device';
    pipeconfTip = 'Show pipeconf view for selected device';

    constructor(
        protected fs: FnService,
        protected ls: LoadingService,
        private is: IconService,
        private ks: KeyService,
        protected log: LogService,
        private mast: MastService,
        private nav: NavService,
        protected wss: WebSocketService,
        @Inject('Window') private window: Window,
    ) {
        super(fs, ls, log, wss, 'device');
        this.responseCallback = this.deviceResponseCb;
    }

    ngOnInit() {
        this.init();
        this.log.debug('DeviceComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('DeviceComponent destroyed');
    }

    deviceResponseCb(data: DeviceTableResponse) {
        this.log.debug('Device response received for ', data.devices.length, 'devices');
    }

}
