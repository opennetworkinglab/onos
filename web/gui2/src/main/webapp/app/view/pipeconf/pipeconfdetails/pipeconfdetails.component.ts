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

import {
    Component, Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges
} from '@angular/core';
import {
    DetailsPanelBaseImpl,
    FnService,
    IconService,
    LogService, WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {PipeconfAction, PipeconfTable} from '../pipeconf/pipeconf.component';
import {animate, state, style, transition, trigger} from '@angular/animations';

/**
 * ONOS GUI -- Pipeconf Detail View Component
 */
@Component({
    selector: 'onos-pipeconfdetails',
    templateUrl: './pipeconfdetails.component.html',
    styleUrls: ['./pipeconfdetails.component.css',
        '../../../../../../../../gui2-fw-lib/lib/widget/panel.css', '../../../../../../../../gui2-fw-lib/lib/widget/panel-theme.css'],
    animations: [
        trigger('pipeconfDetailsState', [
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
export class PipeconfDetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() id: string;
    @Input() pipeconfTable: PipeconfTable;
    @Input() actions: PipeconfAction[] = [];

    constructor(protected fs: FnService,
                protected log: LogService,
                protected is: IconService,
                protected wss: WebSocketService
    ) {
        super(fs, log, wss, 'device');
    }

    ngOnInit() {
        this.init();
        this.log.debug('Pipeconf Details Component initialized:', this.id);
    }

    /**
     * Stop listening to appDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.destroy();
        this.log.debug('Pipeconf Details Component destroyed');
    }

    /**
     * Details Panel Data Request on row selection changes
     * Should be called whenever id changes
     * If id is empty, no request is made
     */
    ngOnChanges(changes: SimpleChanges) {
        if (this.id === undefined || this.id === '') {
            this.closed = false;
        }
    }

    actionDetails(name: string): PipeconfAction {
        for (const action of this.actions) {
            if (action.name === name) {
                return action;
            }
        }
        this.log.debug('Action not found', name);
    }

}
