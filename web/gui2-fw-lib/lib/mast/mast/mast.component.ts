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
import { Component, Input, OnInit, OnDestroy, Inject, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { LionService } from '../../util/lion.service';
import { LogService } from '../../log.service';
import { NavService } from '../../nav/nav.service';
import { WebSocketService } from '../../remote/websocket.service';

/**
 * ONOS GUI -- Masthead Component
 */
@Component({
  selector: 'onos-mast',
  templateUrl: './mast.component.html',
  styleUrls: ['./mast.component.css', './mast.theme.css']
})
export class MastComponent implements OnInit, OnDestroy {
    @Input() username: string;

    lionFn; // Function
    viewMap = new Map<string, string>([]); // A map of app names
    confirmMessage: string = '';
    strongWarning: string = '';

    constructor(
        private lion: LionService,
        private log: LogService,
        public ns: NavService,
        private wss: WebSocketService,
        private router: Router,
        private zone: NgZone,
        @Inject('Window') private window: any,
    ) {
        this.viewMap.set('apps', 'https://wiki.onosproject.org/display/ONOS/GUI+Application+View');
        this.viewMap.set('device', 'https://wiki.onosproject.org/display/ONOS/GUI+Device+View');
        this.viewMap.set('', 'https://wiki.onosproject.org/display/ONOS/The+ONOS+Web+GUI');
    }

    ngOnInit() {
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('mast', () => this.doLion());
            this.log.debug('LION not available when MastComponent initialized');
        } else {
            this.doLion();
        }

        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['guiRemoved', (data) => this.triggerRefresh(data, false) ],
            ['guiAdded', (data) => this.triggerRefresh(data, true) ]
        ]));
        this.log.debug('MastComponent initialized');
    }

    /**
     * Nav component should never be closed, but in case it does, it's
     * safer to tidy up after itself
     */
    ngOnDestroy() {
        this.lion.loadCbs.delete('mast');
    }

    /**
    * Read the LION bundle for App and set up the lionFn
    */
    doLion() {
        this.lionFn = this.lion.bundle('core.fw.Mast');
        if (this.username === undefined) {
            this.username = this.lionFn('unknown_user');
        }
    }

    /**
    * A dummy implementation of the lionFn until the response is received and the LION
    * bundle is received from the WebSocket
    */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }

    directTo() {
        const curId = this.window.location.pathname.replace('/', '');
        let helpUrl: string = this.viewMap.get(curId);
        if (helpUrl === undefined) {
            helpUrl = this.viewMap.get('');
            this.log.warn('No help file linked for view:', curId);
        }
        this.window.open(helpUrl);
    }

    triggerRefresh(data: any, added: boolean): void {
        this.confirmMessage = this.lionFn(added ? 'uicomp_added' : 'uicomp_removed');
        this.log.debug('Refresh has been triggered - item', added ? 'added' : 'removed', ' - ', data);
    }

    /**
     * Callback when the Confirm dialog is shown and a choice is made
     */
    dOk(choice: boolean) {
        if (choice) {
            this.ns.getUiViews();
            this.router.navigate(['/']);
            this.zone.runOutsideAngular(() => {
                location.reload();
            });
            this.log.debug('Refresh confirmed'); // Will not be printed if page reloads

        } else {
            this.log.debug('Refresh cancelled');
        }
        this.confirmMessage = '';
        this.strongWarning = '';
    }
}
