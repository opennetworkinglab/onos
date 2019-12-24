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
import {Component, OnInit} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {LogService} from '../../log.service';
import {FnService} from '../../util/fn.service';
import {KeysService} from '../../util/keys.service';
import {LionService} from '../../util/lion.service';

export interface KeyEntry {
    keystroke: string;
    text: string;
}

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
export class QuickhelpComponent implements OnInit {
    lionFn; // Function
    lionFnTopo; // Function

    dialogKeys: Object;
    globalKeys: Object[];
    maskedKeys: Object;
    viewGestures: Object;
    viewKeys: KeyEntry[][];

    private static extractKeyEntry(viewKeyObj: Object, log: LogService): KeyEntry {
        const subParts = (<any>Object).values(viewKeyObj[1]);
        return <KeyEntry>{
            keystroke: <string>viewKeyObj[0],
            text: <string>subParts[1]
        };
    }

    constructor(
        private log: LogService,
        private fs: FnService,
        public ks: KeysService,
        private lion: LionService
    ) {
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lionFnTopo = this.dummyLion;
            this.lion.loadCbs.set('quickhelp', () => this.doLion());
        } else {
            this.doLion();
        }
        this.globalKeys = [];
        this.viewKeys = [[], [], [], [], [], [], [], [], []];

        this.log.debug('QuickhelpComponent constructed');
    }

    ngOnInit(): void {
        (<any>Object).entries(this.ks.keyHandler.viewKeys)
            .filter((vk) => vk[0] !== '_helpFormat' && vk[0] !== '9' && vk[0] !== 'esc')
            .forEach((vk, idx) => {
                const ke = QuickhelpComponent.extractKeyEntry(vk, this.log);
                this.viewKeys[Math.floor(idx / 3)][idx % 3] = ke;
            });
        this.log.debug('QuickhelpComponent initialized');
        this.log.debug('view keys retrieved', this.ks.keyHandler.globalKeys);
    }


    /**
     * Read the LION bundle for Toolbar and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.fw.QuickHelp');
        this.lionFnTopo = this.lion.bundle('core.view.Topo');
    }

    /**
    * A dummy implementation of the lionFn until the response is received and the LION
    * bundle is received from the WebSocket
    */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
