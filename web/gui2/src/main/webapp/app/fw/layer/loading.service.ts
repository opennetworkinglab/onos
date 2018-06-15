/*
 *  Copyright 2015-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { Injectable } from '@angular/core';
import { FnService } from '../util/fn.service';
import { LogService } from '../../log.service';
import { ThemeService } from '../util/theme.service';
import { WebSocketService } from '../remote/websocket.service';
import * as d3 from 'd3';

const id = 'loading-anim';
const dir = 'data/img/loading/';
const pfx = '/load-';
const nImgs = 16;
const speed = 100;
const waitDelay = 500;


/**
 * ONOS GUI -- Layer -- Loading Service
 *
 * Provides a mechanism to start/stop the loading animation, center screen.
 */
@Injectable({
  providedIn: 'root',
})
export class LoadingService {
    images: any[] = [];
    idx = 0;
    img: any;
    theme: string;
    task: any;
    wait: number;

    constructor(
        private fs: FnService,
        private log: LogService,
        private ts: ThemeService,
        private wss: WebSocketService
    ) {
        this.preloadImages();
        this.log.debug('LoadingService constructed');
    }

    dbg(...args) {
        this.fs.debug(this.constructor.name, args);
    }

    preloadImages() {
        let idx: number;

        this.dbg('preload images start...');
        for (idx = 1; idx <= nImgs ; idx++) {
            this.addImg('light', idx);
            this.addImg('dark', idx);
        }
        this.dbg('preload images DONE!', this.images);
    }

    addImg(theme: string, idx: number) {
        const img = new Image();
        img.src = this.fname(idx, theme);
        this.images.push(img);
    }

    fname(i: number, theme: string) {
        const z = i > 9 ? '' : '0';
        return dir + theme + pfx + z + i + '.png';
    }

    nextFrame() {
        this.idx = this.idx === 16 ? 1 : this.idx + 1;
        this.img.attr('src', this.fname(this.idx, this.theme));
    }

    // start displaying 'loading...' animation (idempotent)
    startAnim() {
        this.dbg('start ANIMATION');
        this.theme = this.ts.getTheme();
        let div = d3.select('#' + id);
        if (div.empty()) {
            div = d3.select('body')
                .append('div')
                .attr('id', id);
            this.img = div
                .append('img')
                .attr('src', this.fname(1, this.theme));
            this.idx = 1;
            this.task = setInterval(() => this.nextFrame(), speed);
        }
    }

    // stop displaying 'loading...' animation (idempotent)
    stopAnim() {
        this.dbg('*stop* ANIMATION');
        if (this.task) {
            clearInterval(this.task);
            this.task = null;
        }
        d3.select('#' + id).remove();
    }

    // schedule function to start animation in the future
    start() {
        this.dbg('start (schedule)');
        this.wait = setTimeout(this.startAnim(), waitDelay);
    }

    // cancel future start, if any; stop the animation
    stop() {
        if (this.wait) {
            this.dbg('start CANCELED');
            clearTimeout(this.wait);
            this.wait = null;
        }
        this.stopAnim();
    }

    // return true if start() has been called but not stop()
    waiting(): boolean {
        return !!this.wait;
    }


}
