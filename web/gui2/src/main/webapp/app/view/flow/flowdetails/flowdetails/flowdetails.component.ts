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
import { Component, OnInit, OnDestroy, OnChanges, Input, Output, EventEmitter } from '@angular/core';
import {
    FnService,
    LionService,
    LogService,
    DetailsPanelBaseImpl,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { trigger, state, style, transition, animate } from '@angular/animations';

/**
 * The details view when a flow is clicked from the flows view
 *
 * This is expected to be passed an 'id' and it makes a call
 * to the WebSocket with a flowDetailsRequest, and gets back a
 * flowDetailsResponse.
 *
 * The animated fly-in is controlled by the animation below
 * The flowDetailsState is attached to flow-details-panel
 * and is false (flies out) when id='' and true (flies in) when
 * id has a value
 */
@Component({
    selector: 'onos-flowdetails',
    templateUrl: './flowdetails.component.html',
    styleUrls: [
        './flowdetails.component.css',
        '../../../../../../../../../gui2-fw-lib/lib/widget/panel.css', '../../../../../../../../../gui2-fw-lib/lib/widget/panel-theme.css'
    ],
    animations: [
        trigger('flowDetailsState', [
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
export class FlowDetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {

    @Input() flowId: string;
    @Input() appId: string;

    @Output() closeEvent = new EventEmitter<string>();

    lionFn; // Function

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected lion: LionService,
    ) {
        super(fs, log, wss, 'flow');
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('flowdetails', () => this.doLion());
        } else {
            this.doLion();
        }
    }

    /**
       * There is a possibility that a previous selection
       * is already registered for call - if so wait 100ms
       * for it to deregister - this is because in the list of
       * flows we might have selected one higher up the list and
       * it is now being processed here before an older selection
       * farther down the list has been removed
       */
    ngOnInit() {
        this.init();
        this.log.debug('Flow Details Component initialized:', this.flowId);
    }

    /**
     * Stop listening to flowDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.lion.loadCbs.delete('flowdetails');
        this.destroy();
        this.log.debug('Flow Details Component destroyed');
    }

    /**
     * Details Panel Data Request on row selection changes
     * Should be called whenever flow id changes
     * If flowId or appId is empty, no request is made
     */
    ngOnChanges() {
        if (this.flowId === '' || this.appId === '') {
            return;
        } else {
            const query = {
                'flowId': this.flowId,
                'appId': this.appId
            };
            this.requestDetailsPanelData(query);
        }
    }

    /**
     * Read the LION bundle for Flow and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Flow');
    }

    /**
     * Return immediate value of flow treatment on flow details request
     */
    immed(treatmentData: any) {
        if (treatmentData === undefined) {
            return '';
        } else {
            return treatmentData.immed;
        }
    }

    /**
     * Return clear deferred value of flow treatment on flow details request
     */
    clearDef(treatmentData: any) {
        if (treatmentData === undefined) {
            return '';
        } else {
            return treatmentData.clearDef;
        }
    }

    close() {
        this.flowId = null;
        this.appId = null;
        this.closed = true;
        this.closeEvent.emit(this.flowId);
    }
}
