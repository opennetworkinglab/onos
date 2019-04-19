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
import {FnService, KeysService, LogService, LionService, WebSocketService, WsOptions} from 'gui2-fw-lib';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
    title = 'Test app for GUI Framework Library';
    loadingRunning: boolean = false;
    lionFn; // Function

    constructor(
        protected fs: FnService,
        protected ks: KeysService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected lion: LionService
    ) {
        this.wss.createWebSocket(<WsOptions>{ wsport: 8181});
        this.title = this.fs.cap(this.title);

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-toolbar', () => this.doLion());
        } else {
            this.doLion();
        }

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
            A: [() => {this.showKeyStroke('A'); }, this.lionFn('tr_btn_monitor_all')],
            B: [(token) => {this.showKeyStroke('B'); }, this.lionFn('tbtt_tog_map')],
            D: [(token) => {this.showKeyStroke('D'); }, this.lionFn('tbtt_tog_use_detail')],
            E: [() => {this.showKeyStroke('E'); }, this.lionFn('tbtt_eq_master')],
            H: [() => {this.showKeyStroke('H'); }, this.lionFn('tbtt_tog_host')],
            I: [(token) => {this.showKeyStroke('I'); }, this.lionFn('tbtt_tog_instances')],
            G: [() => {this.showKeyStroke('G'); }, this.lionFn('tbtt_sel_map')],
            L: [() => {this.showKeyStroke('L'); }, this.lionFn('tbtt_cyc_dev_labs')],
            M: [() => {this.showKeyStroke('M'); }, this.lionFn('tbtt_tog_offline')],
            O: [() => {this.showKeyStroke('O'); }, this.lionFn('tbtt_tog_summary')],
            P: [(token) => {this.showKeyStroke('P'); }, this.lionFn('tbtt_tog_porthi')],
            Q: [() => {this.showKeyStroke('Q'); }, this.lionFn('tbtt_cyc_grid_display')],
            R: [() => {this.showKeyStroke('R'); }, this.lionFn('tbtt_reset_zoom')],
            U: [() => {this.showKeyStroke('U'); }, this.lionFn('tbtt_unpin_node')],
            X: [() => {this.showKeyStroke('X'); }, this.lionFn('tbtt_reset_loc')],
            dot: [() => {this.showKeyStroke('.'); }, this.lionFn('tbtt_tog_toolbar')],
            0: [() => {this.showKeyStroke('0'); }, this.lionFn('tr_btn_cancel_monitoring')],
            'shift-L': [() => {this.showKeyStroke('shift-L'); }, this.lionFn('tbtt_cyc_host_labs')],

            // -- instance color palette debug
            9: () => {
                this.showKeyStroke('9');
            },

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

    /**
     * Read the LION bundle for Toolbar and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Topo');
    }

    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
