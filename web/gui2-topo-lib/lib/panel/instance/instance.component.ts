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
import {
    Component,
    Input,
    Output,
    EventEmitter, OnChanges, SimpleChanges
} from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    LogService,
    FnService,
    PanelBaseImpl,
    IconService,
    SvgUtilService, LionService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * A model of instance information that drives each panel
 */
export interface Instance {
    id: string;
    ip: string;
    online: boolean;
    ready: boolean;
    switches: number;
    uiAttached: boolean;
}

/**
 * ONOS GUI -- Topology Instances Panel.
 * Displays ONOS instances. The onosInstances Array gets updated by topology.service
 * whenever a topo2AllInstances update arrives back on the WebSocket
 *
 * This emits a mastership event when the user clicks on an instance, to
 * see the devices that it has mastership of.
 */
@Component({
    selector: 'onos-instance',
    templateUrl: './instance.component.html',
    styleUrls: [
        './instance.component.css', './instance.theme.css',
        '../../topology.common.css',
        '../../../../gui2-fw-lib/lib/widget/panel.css',
        '../../../../gui2-fw-lib/lib/widget/panel-theme.css'
    ],
    animations: [
        trigger('instancePanelState', [
            state('true', style({
                transform: 'translateX(0%)',
                opacity: '1.0'
            })),
            state('false', style({
                transform: 'translateX(-100%)',
                opacity: '0.0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})
export class InstanceComponent extends PanelBaseImpl implements OnChanges {
    @Input() onosInstances: Instance[] = [];
    @Input() divTopPx: number = 100;
    @Input() on: boolean = false; // Override the parent class attribute
    @Output() mastershipEvent = new EventEmitter<string>();
    public mastership: string;
    lionFn; // Function

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected is: IconService,
        protected sus: SvgUtilService,
        private lion: LionService
    ) {
        super(fs, log);

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-inst', () => this.doLion());
        } else {
            this.doLion();
        }
        this.log.debug('InstanceComponent constructed');
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['onosInstances']) {
            this.onosInstances = <Instance[]>changes['onosInstances'].currentValue;
        }
    }

    /**
     * Get a colour for the banner of the nth panel
     * @param idx The index of the panel (0-6)
     */
    panelColour(idx: number): string {
        return this.sus.cat7().getColor(idx, false, '');
    }

    /**
     * Toggle the display of mastership
     * If the same instance is clicked a second time then cancel display of mastership
     * @param instId The instance to display mastership for
     */
    chooseMastership(instId: string): void {
        if (this.mastership === instId) {
            this.mastership = undefined;
        } else {
            this.mastership = instId;
        }
        this.mastershipEvent.emit(this.mastership);
        this.log.debug('Instance', this.mastership, 'chosen on GUI');
    }

    /**
     * Read the LION bundle for Details panel and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Topo');

    }
}
