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
import { Component, Input, OnInit, OnDestroy, OnChanges } from '@angular/core';
import { trigger, state, style, animate, transition } from '@angular/animations';
import {
    FnService,
    IconService,
    LogService,
    DetailsPanelBaseImpl,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * The details view when a device row is clicked from the Device view
 *
 * This is expected to be passed an 'id' and it makes a call
 * to the WebSocket with an deviceDetailsRequest, and gets back an
 * deviceDetailsResponse.
 *
 * The animated fly-in is controlled by the animation below
 * The deviceDetailsState is attached to device-details-panel
 * and is false (flies out) when id='' and true (flies in) when
 * id has a value
 */
@Component({
    selector: 'onos-devicedetails',
    templateUrl: './devicedetails.component.html',
    styleUrls: ['./devicedetails.component.css',
        '../../../../../../../../gui2-fw-lib/lib/widget/panel.css', '../../../../../../../../gui2-fw-lib/lib/widget/panel-theme.css'
    ],
    animations: [
        trigger('deviceDetailsState', [
            state('true', style({
                transform: 'translateX(-100%)',
                opacity: '100'
            })),
            state('false', style({
                transform: 'translateX(0%)',
                opacity: '0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})


export class DeviceDetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() id: string;

    constructor(protected fs: FnService,
        protected log: LogService,
        protected is: IconService,
        protected wss: WebSocketService
    ) {
        super(fs, log, wss, 'device');
    }

    ngOnInit() {
        this.init();
        this.log.debug('Device Details Component initialized:', this.id);
    }

    /**
     * Stop listening to appDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.destroy();
        this.log.debug('Device Details Component destroyed');
    }

    /**
     * Details Panel Data Request on row selection changes
     * Should be called whenever id changes
     * If id is empty, no request is made
     */
    ngOnChanges() {
        if (this.id === '') {
            return '';
        } else {
            const query = {
                'id': this.id
            };
            this.requestDetailsPanelData(query);
        }
    }

}
