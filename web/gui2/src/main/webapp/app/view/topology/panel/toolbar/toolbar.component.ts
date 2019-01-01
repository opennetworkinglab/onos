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
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {
    LogService,
    LoadingService,
    FnService,
    PanelBaseImpl, LionService
} from 'gui2-fw-lib';
import {animate, state, style, transition, trigger} from '@angular/animations';

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
        '../../../../fw/widget/panel.css', '../../../../fw/widget/panel-theme.css',
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
export class ToolbarComponent extends PanelBaseImpl implements OnInit {
    // deferred localization strings
    lionFn; // Function
    // Used to drive the display of the hosts icon - there is also another such variable on the forcesvg
    @Input() hostsVisible: boolean = false;
    @Input() instancesVisible: boolean = true;
    @Input() summaryVisible: boolean = true;
    @Input() detailsVisible: boolean = true;
    @Input() backgroundVisible: boolean = false;
    @Input() portsVisible: boolean = true;

    @Output() buttonEvent = new EventEmitter<string>();

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ls: LoadingService,
        private lion: LionService
    ) {
        super(fs, ls, log);
        this.on = false;

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-toolbar', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.debug('ToolbarComponent constructed');
    }

    ngOnInit() {
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
            case 'hosts-tog':
                this.hostsVisible = !this.hostsVisible;
                break;
            case 'instance-tog':
                this.instancesVisible = !this.instancesVisible;
                break;
            case 'summary-tog':
                this.summaryVisible = !this.summaryVisible;
                break;
            case 'details-tog':
                this.detailsVisible = !this.detailsVisible;
                break;
            case 'bkgrnd-tog':
                this.backgroundVisible = !this.backgroundVisible;
                break;
            default:
                this.log.warn('Unhandled toolbar click', name);
        }
        // Send a message up to let TopologyComponent know of the event
        this.buttonEvent.emit(name);
    }
}
