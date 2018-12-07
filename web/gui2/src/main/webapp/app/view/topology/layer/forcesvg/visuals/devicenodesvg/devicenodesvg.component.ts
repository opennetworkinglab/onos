/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    ChangeDetectorRef,
    Component,
    ElementRef, EventEmitter,
    Input,
    OnChanges, Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import {Node, Device, LabelToggle} from '../../models';
import {LogService} from 'gui2-fw-lib';
import {NodeVisual} from '../nodevisual';

/**
 * The Device node in the force graph
 *
 * Note: here the selector is given square brackets [] so that it can be
 * inserted in SVG element like a directive
 */
@Component({
    selector: '[onos-devicenodesvg]',
    templateUrl: './devicenodesvg.component.html',
    styleUrls: ['./devicenodesvg.component.css'],
    // changeDetection: ChangeDetectionStrategy.Default,
    // animations: [
    //     trigger('deviceLabelToggle', [
    //         state('0', style({ // none
    //             width: '36px',
    //         })),
    //         state('1, 2', // id
    //             style({ width: '{{ txtWidth }}'}),
    //             { params: {'txtWidth': '36px'}}
    //         ), // default
    //         transition('0 => *', animate('1000ms ease-in')),
    //         transition('* => 0', animate('1000ms ease-out'))
    //     ])
    // ]
})
export class DeviceNodeSvgComponent extends NodeVisual implements OnChanges {
    @Input() device: Device;
    @Input() scale: number = 1.0;
    @Input() labelToggle: LabelToggle = LabelToggle.NONE;
    @Output() selectedEvent = new EventEmitter<Node>();
    textWidth: number = 36;
    @ViewChild('idTxt') idTxt: ElementRef;
    constructor(
        protected log: LogService,
        private ref: ChangeDetectorRef
    ) {
        super();
    }

    /**
     * Called by parent (forcesvg) when a change happens
     *
     * There is a difficulty in passing the SVG text object to the animation
     * directly, to get its width, so we capture it here and update textWidth
     * local variable here and use it in the animation
     */
    ngOnChanges(changes: SimpleChanges) {
        if (changes['device']) {
            if (!this.device.x) {
                this.device.x = 0;
                this.device.y = 0;
            }
        }
        if (changes['labelToggle']) {
            this.labelToggle = changes['labelToggle'].currentValue;
        }
        this.ref.markForCheck();
    }
}
