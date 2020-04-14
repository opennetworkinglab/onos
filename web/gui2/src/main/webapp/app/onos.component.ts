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
import {
    Component,
    OnInit,
    AfterViewInit,
    OnDestroy,
    Inject
} from '@angular/core';
import { Observable, Subscription, fromEvent } from 'rxjs';
import * as d3 from 'd3';
import {
    LionService,
    LogService,
    ThemeService,
    GlyphService,
    WebSocketService,
    WsOptions,
    KeysService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { OnosService, View } from './onos.service';

// secret sauce
const sauce: string[] = [
    '6:69857666',
    '9:826970',
    '22:8069828667',
    '6:698570688669887967',
    '7:6971806889847186',
    '22:8369867682',
    '13:736583',
    '7:667186698384',
    '1:857780888778876787',
    '20:70717066',
    '24:886774868469',
    '17:7487696973687580739078',
    '14:70777086',
    '17:7287687967',
    '11:65678869706783687184',
    '1:80777778',
    '9:72696982',
    '7:857165828967',
    '11:8867696882678869759071'
    // Add more sauce...
];

function cap(s: string): string {
    return s ? s[0].toUpperCase() + s.slice(1) : s;
}

/**
 * The main ONOS Component - the root of the whole user interface
 */
@Component({
  selector: 'onos-root',
  templateUrl: './onos.component.html',
  styleUrls: ['./onos.component.css', './onos.common.css']
})
export class OnosComponent implements OnInit, AfterViewInit, OnDestroy {
    private quickHelpSub: Subscription;
    private quickHelpHandler: Observable<string>;

    // view ID to help page url map.. injected via the servlet
    viewMap: View[]  = [
        // {INJECTED-VIEW-DATA-START}
        // {INJECTED-VIEW-DATA-END}
    ];

    defaultView = 'topo';
    // TODO Move this to OnosModule - warning servlet will have to be updated too
    viewDependencies: string[] = [];

    constructor (
        private lion: LionService,
        private ts: ThemeService,
        private gs: GlyphService,
        private ks: KeysService,
        public wss: WebSocketService,
        private log: LogService,
        public onos: OnosService,
        @Inject('Window') private window: any
    ) {

// This is not like onos.js of AngularJS 1.x In this new structure modules are
// imported instead in the OnosModule. view dependencies should be there too
//        const moduleDependencies = coreDependencies.concat(this.viewDependencies);

        // Testing of debugging
        log.debug('OnosComponent: testing logger.debug()');
        log.info('OnosComponent: testing logger.info()');
        log.warn('OnosComponent: testing logger.warn()');
        log.error('OnosComponent: testing logger.error()');

        this.wss.createWebSocket(<WsOptions>{ wsport: this.window.location.port});

        log.debug('OnosComponent constructed');
    }

    ngOnInit() {
        this.viewMap.forEach(view =>
            this.viewDependencies.push('ov' + cap(view.id)));

        this.onos.viewMap = this.viewMap;

        // TODO: Enable this   this.saucy(this.ee, this.ks);
        this.log.debug('OnosComponent initialized');
    }

    /**
     * Start the listener for keystrokes for QuickHelp
     *
     * This creates an observable that listens to the '/','\' and Esc keystrokes
     * anywhere in the web page - it strips the keyCode out of the keystroke
     * and passes this to another observable that filters only for these keystrokes
     * and finally maps these key code to text literals to drive the
     * quick help feature
     */
    ngAfterViewInit() {
        // const keyStrokeHandler =
        //     fromEvent(document, 'keyup').pipe(map((x: KeyboardEvent) => x.keyCode));
        // this.quickHelpHandler = keyStrokeHandler.pipe(
        //     filter(x => {
        //         return [27, 191, 220].includes(x);
        //     })
        // ).pipe(
        //     map(x => {
        //         let direction;
        //         switch (x) {
        //             case 27:
        //                 direction = 'esc';
        //                 break;
        //             case 191:
        //                 direction = 'fwdslash';
        //                 break;
        //             case 220:
        //                 direction = 'backslash';
        //                 break;
        //             default:
        //                 direction = 'esc';
        //         }
        //         return direction;
        //     })
        // );
        //
        // // TODO: Make a Quick Help component popup
        // this.quickHelpSub = this.quickHelpHandler.subscribe((keyname) => {
        //     this.log.debug('Keystroke', keyname);
        // });
        this.ks.installOn(d3.select('body'));
        this.log.debug('OnosComponent after view initialized');
    }

    ngOnDestroy() {
        if (this.wss.isConnected()) {
            this.log.debug('Stopping Web Socket connection');
            this.wss.closeWebSocket();
        }

        this.quickHelpSub.unsubscribe();
        this.log.debug('OnosComponent destroyed');
    }

    saucy(ee, ks) {
        const map1 = ee.genMap(sauce);
        Object.keys(map1).forEach(function (k) {
            ks.addSeq(k, map1[k]);
        });
    }
}
