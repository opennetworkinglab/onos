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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';

export interface NameInputResult {
    chosen: boolean;
    name: string;
}

@Component({
    selector: 'onos-name-input',
    templateUrl: './name-input.component.html',
    styleUrls: [
        './name-input.component.css',
        './name-input.theme.css',
        '../../layer/dialog.css',
        '../../layer/dialog.theme.css',
        '../../widget/panel.css',
        '../../widget/panel-theme.css'
    ],
    animations: [
        trigger('niDlgState', [
            state('true', style({
                transform: 'translateX(-100%)',
                opacity: '100'
            })),
            state('false', style({
                transform: 'translateX(0%)',
                opacity: '0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})
export class NameInputComponent implements OnInit {
    @Input() warning: string;
    @Input() title: string = '';
    @Input() pattern;
    @Input() minLen = 4;
    @Input() maxLen = 40;
    @Input() placeholder = 'name';
    @Output() chosen: EventEmitter<NameInputResult> = new EventEmitter();

    constructor() {
    }

    ngOnInit() {
    }

    /**
     * When OK or Cancel is pressed, send an event to parent with choice
     */
    choice(chosen: boolean, newName: string): void {
        if (chosen && (newName === undefined || newName === '')) {
            return;
        }
        this.chosen.emit(<NameInputResult>{
            chosen: chosen,
            name: newName
        });
    }
}
