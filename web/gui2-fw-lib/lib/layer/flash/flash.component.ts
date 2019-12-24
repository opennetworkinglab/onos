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
import { Component, Input, Output, OnChanges, SimpleChange, EventEmitter } from '@angular/core';
import { LogService } from '../../log.service';
import { trigger, state, style, animate, transition } from '@angular/animations';

/**
 * ONOS GUI -- Layer -- Flash Component
 *
 * Replaces Flash Service in old GUI.
 * Provides a mechanism to flash short informational messages to the screen
 * to alert the user of something, e.g. "Hosts visible" or "Hosts hidden".
 *
 * It can be used in a warning mode, where text will appear in red
 * The dwell time (milliseconds) can be controlled or the default is 1200ms
 *
 * To use add an element to the template like
 *   <onos-flash message="Hosts visible" dwell="2000" warning="true"></onos-flash>
 * This whole element can be disabled until needed with an ngIf, but if this is done
 * the animated fade-in and fade-out will not happen
 * There is also a (closed) event that tells you when the message is closed, or
 * fades-out
 */
@Component({
    selector: 'onos-flash',
    templateUrl: './flash.component.html',
    styleUrls: [
        './flash.component.css',
        '../dialog.css',
        '../dialog.theme.css',
    ],
    animations: [
        trigger('flashState', [
            state('false', style({
//                transform: 'translateY(-400%)',
                opacity: '0.0',
            })),
            state('true', style({
//                transform: 'translateY(0%)',
                opacity: '1.0',
            })),
            transition('0 => 1', animate('200ms ease-in')),
            transition('1 => 0', animate('200ms ease-out'))
        ])
    ]
})
export class FlashComponent implements OnChanges {
    @Input() message: string;
    @Input() dwell: number = 1200; // milliseconds
    @Input() warning: boolean = false;
    @Output() closed: EventEmitter<boolean> = new EventEmitter();

    public visible: boolean = false;

    /**
     * Flash a message up for 1200ms then disappear again.
     * See animation parameter for the ease in and ease out params
     */
    ngOnChanges(changes: {[propertyName: string]: SimpleChange}) {
        if (changes['message'] && this.message && this.message !== '') {
            this.visible = true;

            setTimeout(() => {
                this.visible = false;
                this.closed.emit(false);
            }, this.dwell);
        }
    }

    /**
     * The message will flash up for 'dwell' milliseconds
     * If dwell is > 2000ms, then there will be a button that allows it to be dismissed now
     */
    closeNow() {
        this.visible = false;
        this.closed.emit(false);
    }
}
