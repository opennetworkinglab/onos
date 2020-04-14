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
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges, OnInit, Output,
    SimpleChanges,
} from '@angular/core';
import {
    Badge,
    Device,
    LabelToggle,
} from '../../models';
import {IconService, LogService, SvgUtilService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {NodeVisual, SelectedEvent} from '../nodevisual';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {TopologyService} from '../../../../topology.service';

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
    changeDetection: ChangeDetectionStrategy.Default,
    animations: [
        trigger('deviceLabelToggle', [
            state('0', style({ // none
                width: '36px',
            })),
            state('1, 2', // id
                style({ width: '{{ txtWidth }}'}),
                { params: {'txtWidth': '36px'}}
            ), // default
            transition('0 => 1', animate('250ms ease-in')),
            transition('1 => 2', animate('250ms ease-in')),
            transition('* => 0', animate('250ms ease-out'))
        ]),
        trigger('deviceLabelToggleTxt', [
            state('0', style( {
                opacity: 0,
            })),
            state( '1,2', style({
                opacity: 1.0
            })),
            transition('0 => 1', animate('250ms ease-in')),
            transition('* => 0', animate('250ms ease-out'))
        ])
    ]
})
export class DeviceNodeSvgComponent extends NodeVisual implements OnInit, OnChanges {
    @Input() device: Device;
    @Input() scale: number = 1.0;
    @Input() labelToggle: LabelToggle.Enum = LabelToggle.Enum.NONE;
    @Input() colorMuted: boolean = false;
    @Input() colorTheme: string = 'light';
    @Input() badge: Badge;
    @Output() selectedEvent = new EventEmitter<SelectedEvent>();
    textWidth: number = 36;
    panelColor: string = '#9ebedf';

    constructor(
        protected log: LogService,
        private is: IconService,
        protected sus: SvgUtilService,
        protected ts: TopologyService,
        private ref: ChangeDetectorRef
    ) {
        super();
    }

    ngOnInit(): void {
        this.panelColor = this.panelColour();
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
            // The master might have changed - recalculate color
            this.panelColor = this.panelColour();
        }

        if (changes['colorMuted']) {
            this.colorMuted = changes['colorMuted'].currentValue;
            this.panelColor = this.panelColour();
        }

        if (changes['badge']) {
            this.badge = changes['badge'].currentValue;
        }
    }

    /**
     * Calculate the text length in advance as well as possible
     *
     * The length of SVG text cannot be exactly estimated, because depending on
     * the letters kerning might mean that it is shorter or longer than expected
     *
     * This takes the approach of 8px width per letter of this size, that on average
     * evens out over words. A word like 'ilj' will be much shorter than 'wm0'
     * because of kerning
     *
     *
     * In addition in the template, the <svg:text> properties
     * textLength and lengthAdjust ensure that the text becomes long with extra
     * wide spacing created as necessary.
     *
     * Other approaches like getBBox() of the text
     */
    labelTextLen() {
        if (this.labelToggle === 1) {
            return this.device.id.length * 12;
        } else if (this.labelToggle === 2 && this.device &&
            this.device.props.name && this.device.props.name.trim().length > 0) {
            return this.device.props.name.length * 12;
        } else {
            return 0;
        }
    }

    deviceIcon(): string {
        if (this.device.props && this.device.props.uiType) {
            this.is.loadIconDef(this.device.props.uiType);
            return this.device.props.uiType;
        } else {
            return 'm_' + this.device.type;
        }
    }

    /**
     * Get a colour for the banner of the nth panel
     * @param idx The index of the panel (0-6)
     */
    panelColour(): string {
        const idx = this.ts.instancesIndex.get(this.device.master);
        return this.sus.cat7().getColor(idx, this.colorMuted, this.colorTheme);
    }
}
