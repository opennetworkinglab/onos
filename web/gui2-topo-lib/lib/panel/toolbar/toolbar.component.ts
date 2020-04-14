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
import {
    LogService,
    FnService,
    PanelBaseImpl, LionService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

import {animate, state, style, transition, trigger} from '@angular/animations';

export const INSTANCE_TOGGLE = 'instance-tog';
export const SUMMARY_TOGGLE = 'summary-tog';
export const DETAILS_TOGGLE = 'details-tog';
export const HOSTS_TOGGLE = 'hosts-tog';
export const OFFLINE_TOGGLE = 'offline-tog';
export const PORTS_TOGGLE = 'ports-tog';
export const BKGRND_TOGGLE = 'bkgrnd-tog';
export const BKGRND_SELECT = 'bkgrnd-sel';
export const CYCLELABELS_BTN = 'cycleLabels-btn';
export const CYCLEHOSTLABEL_BTN = 'cycleHostLabel-btn';
export const CYCLEGRIDDISPLAY_BTN = 'cycleGridDisplay-btn';
export const RESETZOOM_BTN = 'resetZoom-btn';
export const EQMASTER_BTN = 'eqMaster-btn';
export const CANCEL_TRAFFIC = 'cancel-traffic';
export const ALL_TRAFFIC = 'all-traffic';
export const QUICKHELP_BTN = 'quickhelp-btn';
export const LAYOUT_DEFAULT_BTN = 'layout-default-btn';
export const LAYOUT_ACCESS_BTN = 'layout-access-btn';
export const ALARMS_TOGGLE = 'alarms-tog';

/*
 ONOS GUI -- Topology Toolbar Module.
 Defines modeling of ONOS toolbar.
 */
@Component({
    selector: 'onos-toolbar',
    templateUrl: './toolbar.component.html',
    styleUrls: [
        './toolbar.component.css', './toolbar.theme.css',
        '../../topology.common.css',
        '../../../../gui2-fw-lib/lib/widget/panel.css',
        '../../../../gui2-fw-lib/lib/widget/panel-theme.css',
        './button.css'
    ],
    animations: [
        trigger('toolbarState', [
            state('true', style({
                transform: 'translateX(0%)',
                // opacity: '1.0'
            })),
            state('false', style({
                transform: 'translateX(-93%)',
                // opacity: '0.0'
            })),
            transition('0 => 1', animate('500ms ease-in')),
            transition('1 => 0', animate('500ms ease-out'))
        ])
    ]
})
export class ToolbarComponent extends PanelBaseImpl {
    @Input() on: boolean = false; // Override the parent class attribute
    // deferred localization strings
    lionFn; // Function
    // Used to drive the display of the hosts icon - there is also another such variable on the forcesvg
    @Input() hostsVisible: boolean = false;
    @Input() instancesVisible: boolean = true;
    @Input() summaryVisible: boolean = true;
    @Input() detailsVisible: boolean = true;
    @Input() backgroundVisible: boolean = false;
    @Input() portsVisible: boolean = true;
    @Input() alarmsVisible: boolean = true;

    @Output() buttonEvent = new EventEmitter<string>();

    constructor(
        protected fs: FnService,
        protected log: LogService,
        private lion: LionService
    ) {
        super(fs, log);

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-toolbar', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.debug('ToolbarComponent constructed');
    }

    /**
     * Read the LION bundle for Toolbar and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Topo');
    }

    /**
     * As buttons are clicked on the toolbar, emit events up to the parent
     *
     * The toggling of the input variables here is in addition to the control
     * of these input variables from the parent. This is so that this component
     * may be reused and is not dependent on a particular parent implementation
     * to work
     * @param name The name of button clicked.
     */
    buttonClicked(name: string): void {
        switch (name) {
            case HOSTS_TOGGLE:
                this.hostsVisible = !this.hostsVisible;
                break;
            case INSTANCE_TOGGLE:
                this.instancesVisible = !this.instancesVisible;
                break;
            case SUMMARY_TOGGLE:
                this.summaryVisible = !this.summaryVisible;
                break;
            case DETAILS_TOGGLE:
                this.detailsVisible = !this.detailsVisible;
                break;
            case BKGRND_TOGGLE:
                this.backgroundVisible = !this.backgroundVisible;
                break;
            case ALARMS_TOGGLE:
                this.alarmsVisible = !this.alarmsVisible;
                break;
            default:
        }
        // Send a message up to let TopologyComponent know of the event
        this.buttonEvent.emit(name);
    }
}
