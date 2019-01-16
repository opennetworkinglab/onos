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
import {Component} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {LogService} from '../../log.service';
import {FnService} from '../../util/fn.service';
import {KeysService} from '../../util/keys.service';
import {LionService} from '../../util/lion.service';


@Component({
    selector: 'onos-quickhelp',
    templateUrl: './quickhelp.component.html',
    styleUrls: ['./quickhelp.component.css'],
    animations: [
        trigger('quickHelpState', [
            state('true', style({
                opacity: '1.0',
            })),
            state('false', style({
                opacity: '0.0',
            })),
            transition('0 => 1', animate('500ms ease-in')),
            transition('1 => 0', animate('500ms ease-out'))
        ])
    ]
})
export class QuickhelpComponent {
    lionFn; // Function

    constructor(
        private log: LogService,
        private fs: FnService,
        public ks: KeysService,
        private lion: LionService
    ) {
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('quickhelp', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.debug('Quickhelp component constructed');
    }

    /**
     * Read the LION bundle for Toolbar and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.fw.QuickHelp');
    }

    /**
    * A dummy implementation of the lionFn until the response is received and the LION
    * bundle is received from the WebSocket
    */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
