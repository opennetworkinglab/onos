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

import {Component} from '@angular/core';
import {FnService, KeysService, LogService} from 'gui2-fw-lib';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
    title = 'Test app for GUI Framework Library';
    loadingRunning: boolean = false;

    constructor(
        protected fs: FnService,
        protected ks: KeysService,
        protected log: LogService
    ) {
        this.title = this.fs.cap(this.title);
        this.bindCommands();
        this.log.debug('AppComponent constructed');
    }

    bindCommands(additional?: any) {

        const am = this.actionMap();
        const add = this.fs.isO(additional);

        this.ks.keyBindings(am);

        this.ks.gestureNotes([
            ['click', 'Select the item and show details'],
            ['shift-click', 'Toggle selection state'],
            ['drag', 'Reposition (and pin) device / host'],
            ['cmd-scroll', 'Zoom in / out'],
            ['cmd-drag', 'Pan'],
        ]);
    }


    showKeyStroke(letter: string, token?: string) {
        this.log.debug('Key pressed', letter);
    }

    actionMap() {
        return {
            A: [() => {this.showKeyStroke('A'); }, 'Monitor all traffic'],
            B: [(token) => {this.showKeyStroke('B', token); }, 'Toggle background'],
            D: [(token) => {this.showKeyStroke('D', token); }, 'Toggle details panel'],
            E: [() => {this.showKeyStroke('E'); }, 'Equalize mastership roles'],
            H: [() => {this.showKeyStroke('H'); }, 'Toggle host visibility'],
            I: [(token) => {this.showKeyStroke('I', token); }, 'Toggle ONOS Instance Panel'],
            G: [() => {this.showKeyStroke('G'); }, 'Show map selection dialog'],
            L: [() => {this.showKeyStroke('L'); }, 'Cycle device labels'],
            M: [() => {this.showKeyStroke('M'); }, 'Toggle offline visibility'],
            O: [() => {this.showKeyStroke('O'); }, 'Toggle the Summary Panel'],
            P: [(token) => {this.showKeyStroke('P', token); }, 'Toggle Port Highlighting'],
            Q: [() => {this.showKeyStroke('Q'); }, 'Cycle grid display'],
            R: [() => {this.showKeyStroke('R'); }, 'Reset pan / zoom'],
            U: [() => {this.showKeyStroke('U'); }, 'Unpin or freeze nodes'],
            X: [() => {this.showKeyStroke('X'); }, 'Reset Node Location'],
            dot: [() => {this.showKeyStroke('.'); }, 'Toggle Toolbar'],
            0: [() => {this.showKeyStroke('0'); }, 'Cancel traffic monitoring'],
            'shift-L': [() => {this.showKeyStroke('shift-L'); }, 'Cycle host labels'],

            esc: [() => {this.showKeyStroke('Esc'); }, 'Cancel commands'],

            // TODO update after adding in Background Service
            // topology overlay selections
            // F1: function () { t2tbs.fnKey(0); },
            // F2: function () { t2tbs.fnKey(1); },
            // F3: function () { t2tbs.fnKey(2); },
            // F4: function () { t2tbs.fnKey(3); },
            // F5: function () { t2tbs.fnKey(4); },
            //
            // _keyListener: t2tbs.keyListener.bind(t2tbs),

            _helpFormat: [
                ['I', 'O', 'D', 'H', 'M', 'P', 'dash', 'B'],
                ['X', 'Z', 'N', 'L', 'shift-L', 'U', 'R', 'E', 'dot'],
                [], // this column reserved for overlay actions
            ],
        };
    }
}
