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
import { Component, OnInit } from '@angular/core';
import { FnService } from '../../util/fn.service';
import { GlyphService } from '../../svg/glyph.service';
import { LogService } from '../../log.service';
import { SvgUtilService } from '../../svg/svgutil.service';
import { WebSocketService } from '../../remote/websocket.service';

const BIRD = 'bird';

/**
 * ONOS GUI -- Layer -- Veil Component
 *
 * Provides a mechanism to display an overlaying div with information.
 * Used mainly for web socket connection interruption.
 *
 * It can be added to an component's template as follows:
 *     <onos-veil #veil></onos-veil>
 *     <p (click)="veil.show(['t1','t2','t3'])">Test Veil</p>
 */
@Component({
  selector: 'onos-veil',
  templateUrl: './veil.component.html',
  styleUrls: ['./veil.component.css', './veil.component.theme.css']
})
export class VeilComponent implements OnInit {
    ww: number;
    wh: number;
    birdSvg: string;
    birdDim: number;
    enabled: boolean = false;
    trans: string;
    messages: string[] = [];
    veilStyle: string;

    constructor(
        public fs: FnService,
        private gs: GlyphService,
        private log: LogService,
        private sus: SvgUtilService,
        private wss: WebSocketService
    ) {
        const wSize = this.fs.windowSize();
        this.ww = wSize.width;
        this.wh = wSize.height;
        const shrink = this.wh * 0.3;
        this.birdDim = this.wh - shrink;
        const birdCenter = (this.ww - this.birdDim) / 2;
        this.trans = this.sus.translate([birdCenter, shrink / 2]);

        this.log.debug('VeilComponent with ' + BIRD + ' constructed');
    }

    ngOnInit() {
    }

    // msg should be an array of strings
    show(msgs: string[]): void {
        this.messages = msgs;
        this.enabled = true;
//        this.ks.enableKeys(false);
    }

    hide(): void {
        this.veilStyle = 'display: none';
//        this.ks.enableKeys(true);
    }


}
