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
import {
    Component,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges
} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {
    DetailsPanelBaseImpl,
    FnService, LionService,
    LoadingService,
    LogService,
    WebSocketService
} from 'gui2-fw-lib';
import {Host, Link, LinkType, UiElement} from '../../layer/forcesvg/models';
import {Params, Router} from '@angular/router';


interface ButtonAttrs {
    gid: string;
    tt: string;
    path: string;
}

const SHOWDEVICEVIEW: ButtonAttrs = {
    gid: 'deviceTable',
    tt: 'tt_ctl_show_device',
    path: 'device',
};
const SHOWFLOWVIEW: ButtonAttrs = {
    gid: 'flowTable',
    tt: 'title_flows',
    path: 'flow',
};
const SHOWPORTVIEW: ButtonAttrs = {
    gid: 'portTable',
    tt: 'tt_ctl_show_port',
    path: 'port',
};
const SHOWGROUPVIEW: ButtonAttrs = {
    gid: 'groupTable',
    tt: 'tt_ctl_show_group',
    path: 'group',
};
const SHOWMETERVIEW: ButtonAttrs = {
    gid: 'meterTable',
    tt: 'tt_ctl_show_meter',
    path: 'meter',
};
const SHOWPIPECONFVIEW: ButtonAttrs = {
    gid: 'pipeconfTable',
    tt: 'tt_ctl_show_pipeconf',
    path: 'pipeconf',
};

interface ShowDetails {
    buttons: string[];
    glyphId: string;
    id: string;
    navPath: string;
    propLabels: Object;
    propOrder: string[];
    propValues: Object;
    title: string;
}
/**
 * ONOS GUI -- Topology Details Panel.
 * Displays details of selected device. When no device is selected the panel slides
 * off to the side and disappears
 */
@Component({
    selector: 'onos-details',
    templateUrl: './details.component.html',
    styleUrls: [
        './details.component.css', './details.theme.css',
        '../../topology.common.css',
        '../../../../fw/widget/panel.css', '../../../../fw/widget/panel-theme.css'
    ],
    animations: [
        trigger('detailsPanelState', [
            state('true', style({
                transform: 'translateX(0%)',
                opacity: '1.0'
            })),
            state('false', style({
                transform: 'translateX(100%)',
                opacity: '0'
            })),
            transition('0 => 1', animate('100ms ease-in')),
            transition('1 => 0', animate('100ms ease-out'))
        ])
    ]
})
export class DetailsComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy, OnChanges {
    @Input() selectedNode: UiElement = undefined; // Populated when user selects node or link
    @Input() on: boolean = false; // Override the parent class attribute

    // deferred localization strings
    lionFn; // Function
    showDetails: ShowDetails; // Will be populated on callback. Cleared if nothing is selected

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ls: LoadingService,
        protected router: Router,
        protected wss: WebSocketService,
        private lion: LionService
    ) {
        super(fs, ls, log, wss, 'topo');

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('flow', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.debug('Topo DetailsComponent constructed');
    }

    /**
     * When the component is initializing set up the handler for callbacks of
     * ShowDetails from the WSS. Set the variable showDetails when ever a callback
     * is made
     */
    ngOnInit(): void {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['showDetails', (data) => {
                    this.showDetails = data;
                    // this.log.debug('showDetails received', data);
                }
            ]
        ]));
        this.log.debug('Topo DetailsComponent initialized');
    }

    /**
     * When the component is being unloaded then unbind the WSS handler.
     */
    ngOnDestroy(): void {
        this.wss.unbindHandlers(['showDetails']);
        this.log.debug('Topo DetailsComponent destroyed');
    }

    /**
     * If changes are detected on the Input param selectedNode, call on WSS sendEvent
     * Note the difference in call to the WSS with requestDetails between a node
     * and a link - the handling is done in TopologyViewMessageHandler#RequestDetails.process()
     *
     * The WSS will call back asynchronously (see fn in ngOnInit())
     *
     * @param changes Simple Changes set of updates
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['selectedNode']) {
            this.selectedNode = changes['selectedNode'].currentValue;
            let type: any;

            if (this.selectedNode === undefined) {
                // Selection has been cleared
                this.showDetails = <ShowDetails>{};
                return;
            }

            if (this.selectedNode.hasOwnProperty('nodeType')) { // For Device, Host, SubRegion
                type = (<Host>this.selectedNode).nodeType;
                this.wss.sendEvent('requestDetails', {
                    id: this.selectedNode.id,
                    class: type,
                });
            } else if (this.selectedNode.hasOwnProperty('type')) { // Must be link
                const link: Link = <Link>this.selectedNode;
                if (<LinkType><unknown>LinkType[link.type] === LinkType.UiEdgeLink) { // Number based enum
                    this.wss.sendEvent('requestDetails', {
                        key: link.id,
                        class: 'link',
                        sourceId: link.epA,
                        targetId: Link.deviceNameFromEp(link.epB),
                        targetPort: link.portB,
                        isEdgeLink: true
                    });
                } else {
                    this.wss.sendEvent('requestDetails', {
                        key: link.id,
                        class: 'link',
                        sourceId: Link.deviceNameFromEp(link.epA),
                        sourcePort: link.portA,
                        targetId: Link.deviceNameFromEp(link.epB),
                        targetPort: link.portB,
                        isEdgeLink: false
                    });
                }
            } else {
                this.log.warn('Unexpected type for selected element', this.selectedNode);
            }
        }
    }

    /**
     * Table of core button attributes to return per button icon
     * @param btnName The name of the button
     * @returns A structure with the button attributes
     */
    buttonAttribs(btnName: string): ButtonAttrs {
        switch (btnName) {
            case 'showDeviceView':
                return SHOWDEVICEVIEW;
            case 'showFlowView':
                return SHOWFLOWVIEW;
            case 'showPortView':
                return SHOWPORTVIEW;
            case 'showGroupView':
                return SHOWGROUPVIEW;
            case 'showMeterView':
                return SHOWMETERVIEW;
            case 'showPipeConfView':
                return SHOWPIPECONFVIEW;
            default:
                return <ButtonAttrs>{
                    gid: btnName,
                    path: btnName
                };
        }
    }

    /**
     * Navigate using Angular Routing. Combines the parameters to generate a relative URL
     * e.g. if params are 'meter', 'device' and 'null:0000000000001' then the
     * navigation URL will become "http://localhost:4200/#/meter?devId=null:0000000000000002"
     *
     * @param path The path to navigate to
     * @param navPath The parameter name to use
     * @param selId the parameter value to use
     */
    navto(path: string, navPath: string, selId: string): void {
        this.log.debug('navigate to', path, 'for', navPath, '=', selId);
        // Special case until it's fixed
        if (selId) {
            if (navPath === 'device') {
                navPath = 'devId';
            }
            const queryPar: Params = {};
            queryPar[navPath] = selId;
            this.router.navigate([path], { queryParams: queryPar });
        }
    }

    /**
     * Read the LION bundle for Details panel and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Flow');

    }

}
