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
import { Component } from '@angular/core';
import { LogService } from '../../../log.service';
import { trigger, state, style, animate, transition } from '@angular/animations';

/**
 * ONOS GUI -- Layer -- Flash Component
 *
 * Replaces Flash Service in old GUI.
 * Provides a mechanism to flash short informational messages to the screen
 * to alert the user of something, e.g. "Hosts visible" or "Hosts hidden".
 *
 * To use add an element to the template like
 *   <onos-flash #flashComponent (click)="flashComponent.flash('Hosts visible')"></onos-flash>
 * 1) The (click) can be removed and the call to flash() can be called from anywhere else in the template.
 * 2) This whole element can be disabled until needed with an ngIf
 */
@Component({
    selector: 'onos-flash',
    templateUrl: './flash.component.html',
    styleUrls: ['./flash.component.css'],
    animations: [
        trigger('flashState', [
            state('inactive', style({
                opacity: '0.0',
            })),
            state('active', style({
                opacity: '1.0',
            })),
            transition('inactive => active', animate('200ms ease-in')),
            transition('active => inactive', animate('200ms ease-out'))
        ])
    ]
})
export class FlashComponent {
    public message: string;

    public width: string = '100%';
    public height: number = 200;
    public rx: number = 10;
    public vbox: string = '-200 -' + (this.height / 2) + ' 400 ' + this.height;
    public xpad: number = 20;
    public ypad: number = 10;
    public enabled: boolean = false;

    constructor(
        private log: LogService,
    ) {
        this.log.debug('FlashComponent constructed');
    }

    /**
     * Flash a message up for 1200ms then disappear again.
     * See animation parameter for the ease in and ease out params
     */
    flash(message: string): void {
        this.message = message;
        this.enabled = true;

        setTimeout(() => {
            this.enabled = false;
        }, 1200);
    }
}
