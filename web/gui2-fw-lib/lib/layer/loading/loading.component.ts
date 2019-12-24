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
import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {LogService} from '../../log.service';
import {animate, state, style, transition, trigger} from '@angular/animations';

const LOADING_IMG_DIR = 'data/img/loading/';
const LOADING_PFX = '/load-';
const NUM_IMGS = 16;

/**
 * ONOS GUI - A component that shows the loading icon
 *
 * Should be shown if someone has to wait for more than
 * a certain time for data to be retrieved
 * Note the animation - there is a pause of 500ms before the images appear
 * and then it eases in over 200ms
 */
@Component({
    selector: 'onos-loading',
    templateUrl: './loading.component.html',
    styleUrls: ['./loading.component.css'],
    animations: [
        trigger('loadingState', [
            state('false', style({
                opacity: '0.0',
                'z-index': -5000
            })),
            state('true', style({
                opacity: '1.0',
                'z-index': 5000
            })),
            transition('0 => 1', animate('200ms 500ms ease-in')),
            transition('1 => 0', animate('200ms ease-out'))
        ])
    ]
})
export class LoadingComponent implements OnChanges {
    @Input() theme: string = 'light';
    @Input() running: boolean;

    speed: number = 8; // Frames per second
    idx = 1;
    images: HTMLImageElement[] = [];
    img: string;
    task: any;

    constructor(
        private log: LogService,
    ) {
        let idx: number;

        for (idx = 1; idx <= NUM_IMGS ; idx++) {
            this.addImg('light', idx);
            this.addImg('dark', idx);
        }

        this.log.debug('LoadingComponent constructed - images preloaded from', this.fname(1, this.theme));
    }

    /**
     * Detects changes in in Input variable
     * Here we want to detect if running has been enabled or disabled
     * @param changes
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['running']) {
            const newRunning: boolean = changes['running'].currentValue;

            if (newRunning) {
                this.task = setInterval(() => this.nextFrame(), 1000 / this.speed);
            } else {
                if (this.task) {
                    clearInterval(this.task);
                    this.task = null;
                }
            }
        }
    }

    private addImg(theme: string, idx: number): void {
        const img = new Image();
        img.src = this.fname(idx, theme);
        this.images.push(img);
    }

    private fname(i: number, theme: string): string {
        const z = i > 9 ? '' : '0';
        return LOADING_IMG_DIR + theme + LOADING_PFX + z + i + '.png';
    }

    private nextFrame(): void {
        this.idx = this.idx === 16 ? 1 : this.idx + 1;
        this.img  = this.fname(this.idx, this.theme);
    }

}
