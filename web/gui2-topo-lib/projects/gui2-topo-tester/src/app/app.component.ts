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
import {AfterViewInit, Component, OnDestroy} from '@angular/core';
import {
    GlyphService,
    KeysService,
    LionService,
    LogService,
    ThemeService,
    WebSocketService,
    WsOptions
} from 'gui2-fw-lib';
import * as d3 from 'd3';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements AfterViewInit, OnDestroy {
    title = 'gui2-topo-tester';

    constructor (
        private lion: LionService,
        private ts: ThemeService,
        private gs: GlyphService,
        private ks: KeysService,
        public wss: WebSocketService,
        private log: LogService,
    ) {
        // Testing of debugging
        log.debug('OnosComponent: testing logger.debug()');
        log.info('OnosComponent: testing logger.info()');
        log.warn('OnosComponent: testing logger.warn()');
        log.error('OnosComponent: testing logger.error()');

        this.wss.createWebSocket(<WsOptions>{ wsport: 8181});

        log.debug('OnosComponent constructed');
    }

    ngAfterViewInit(): void {
        this.ks.installOn(d3.select('body'));
        this.log.debug('AppComponent after view initialized');
    }


    ngOnDestroy() {
        if (this.wss.isConnected()) {
            this.log.debug('Stopping Web Socket connection');
            this.wss.closeWebSocket();
        }

        this.log.debug('AppComponent destroyed');
    }
}
