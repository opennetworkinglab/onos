/*
 * Copyright 2020-present Open Networking Foundation
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

import {
    Component,
    Input,
    OnInit,
    OnDestroy,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import {trigger, state, style, animate, transition} from '@angular/animations';
import {
    FnService,
    IconService,
    LogService,
    DetailsPanelBaseImpl,
    WebSocketService,
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

@Component({
    selector: 'onos-yangdetails',
    templateUrl: './yangdetails.component.html',
    styleUrls: [
        './yangdetails.component.scss', './yangdetails.theme.scss',
        '../../../../../web/gui2-fw-lib/lib/widget/panel.css',
        '../../../../../web/gui2-fw-lib/lib/widget/panel-theme.css'
    ],
    animations: [
        trigger('yangDetailsState', [
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
export class YangDetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() id: string;
    @Input() modelId: string;
    @Input() revision: string;

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected is: IconService,
        protected wss: WebSocketService
    ) {
        super(fs, log, wss, 'yangModel');
    }

    ngOnInit() {
        this.init();
        this.log.debug('Yang Table Details Component initialized:', this.id);
    }

    /**
     * Stop listening to alarmTableDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.destroy();
        this.log.debug('Yang Table Details Component destroyed');
    }

    /**
     * Details Panel Data Request on row selection changes
     * Should be called whenever id changes
     * If id is empty, no request is made
     */
    ngOnChanges(changes: SimpleChanges) {
        console.log('Change happened', changes);
        if (changes['id']) {
            if (this.id === '') {
                this.modelId = '';
                this.revision = '';
                return '';
            } else {
                const query = {
                    'id': this.id,
                    'modelId': this.modelId,
                    'revision': this.revision
                };
                this.requestDetailsPanelData(query);
            }
        }
    }
}
