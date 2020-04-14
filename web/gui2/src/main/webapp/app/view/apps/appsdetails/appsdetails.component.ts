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
    LionService,
    LogService,
    DetailsPanelBaseImpl,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

import { App, APPURLPREFIX, ICONURLSUFFIX } from '../apps/apps.component';

/**
 * The details view when an app is clicked from the apps view
 *
 * This is expected to be passed an 'id' and it makes a call
 * to the WebSocket with an appDetailsRequest, and gets back an
 * appDetailsResponse.
 *
 * The animated fly-in is controlled by the animation below
 * The appDetailsState is attached to application-details-panel
 * and is false (flies out) when id='' and true (flies in) when
 * id has a value
 */
@Component({
  selector: 'onos-appsdetails',
  templateUrl: './appsdetails.component.html',
  styleUrls: [
    './appsdetails.component.css',
    '../../../../../../../../gui2-fw-lib/lib/widget/panel.css', '../../../../../../../../gui2-fw-lib/lib/widget/panel-theme.css'
  ],
  animations: [
    trigger('appDetailsState', [
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
export class AppsDetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() id: string;

    lionFn; // Function
    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected lion: LionService,
    ) {
        super(fs, log, wss, 'app');
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('appsdetails', () => this.doLion());
        } else {
            this.doLion();
        }
    }

    /**
     * There is a possibility that a previous selection
     * is already registered for call - if so wait 100ms
     * for it to deregister - this is because in the list of
     * apps we might have selected one higher up the list and
     * it is now being processed here before an older selection
     * farther down the list has been removed
     */
    ngOnInit() {
        this.init();
        this.log.debug('App Details Component initialized:', this.id);
    }

    /**
     * Stop listening to appDetailsResponse on WebSocket
     */
    ngOnDestroy() {
        this.lion.loadCbs.delete('appsdetails');
        this.destroy();
        this.log.debug('App Details Component destroyed');
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

    iconUrl(appId: string): string {
        return APPURLPREFIX + appId + ICONURLSUFFIX;
    }

    /**
     * Read the LION bundle for App and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.App');
    }

}
