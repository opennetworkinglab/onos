/*
 * Copyright 2018-present Open Networking Foundation
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
import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    LogService,
    LoadingService,
    FnService,
    DetailsPanelBaseImpl,
    WebSocketService
} from 'gui2-fw-lib';

/*
 ONOS GUI -- Topology Details Panel.
 Displays details of selected device.
 */
@Component({
    selector: 'onos-details',
    templateUrl: './details.component.html',
    styleUrls: [
        './details.component.css', './details.theme.css',
        '../../topology.common.css',
        '../../../../fw/widget/panel.css', '../../../../fw/widget/panel-theme.css'
    ],
    animations: [
        trigger('detailsPanelState', [
            state('true', style({
                transform: 'translateX(0%)',
                opacity: '100'
            })),
            state('false', style({
                transform: 'translateX(100%)',
                opacity: '0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})
export class DetailsComponent extends DetailsPanelBaseImpl implements OnInit {

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ls: LoadingService,
        protected wss: WebSocketService,
    ) {
        super(fs, ls, log, wss, 'topo');
        this.log.debug('InstanceComponent constructed');
    }

    ngOnInit() {
        this.on = false;
    }

}
