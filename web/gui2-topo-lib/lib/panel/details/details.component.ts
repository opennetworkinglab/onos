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
import {animate, state, style, transition, trigger} from '@angular/animations';
import {DetailsPanelBaseImpl, FnService, LionService, LogService, WebSocketService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {Host, Link, LinkType, NodeType, UiElement} from '../../layer/forcesvg/models';
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
const RELATEDINTENTS: ButtonAttrs = {
    gid: 'm_relatedIntents',
    tt: 'tr_btn_show_related_traffic',
    path: 'relatedIntents',
};
const CREATEHOSTTOHOSTFLOW: ButtonAttrs = {
    gid: 'm_endstation',
    tt: 'tr_btn_create_h2h_flow',
    path: 'create_h2h_flow',
};
const CREATEMULTISOURCEFLOW: ButtonAttrs = {
    gid: 'm_flows',
    tt: 'tr_btn_create_msrc_flow',
    path: 'create_msrc_flow',
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
 *
 * This Panel is a child of the Topology component and it gets the 'selectedNodes'
 * from there as an input component. See TopologyComponent.nodeSelected()
 * The topology component gets these by listening to events from ForceSvgComponent
 * which gets them in turn from Device, Host, SubRegion and Link components. This
 * is so that each component respects the hierarchy
 */
@Component({
    selector: 'onos-details',
    templateUrl: './details.component.html',
    styleUrls: [
        './details.component.css', './details.theme.css',
        '../../topology.common.css',
        '../../../../gui2-fw-lib/lib/widget/panel.css',
        '../../../../gui2-fw-lib/lib/widget/panel-theme.css'
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
    @Input() selectedNodes: UiElement[] = []; // Populated when user selects node or link
    @Input() on: boolean = false; // Override the parent class attribute

    // deferred localization strings
    lionFnTopo; // Function
    lionFnFlow; // Function for flow bundle
    showDetails: ShowDetails; // Will be populated on callback. Cleared if nothing is selected

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected router: Router,
        protected wss: WebSocketService,
        private lion: LionService
    ) {
        super(fs, log, wss, 'topo');

        if (this.lion.ubercache.length === 0) {
            this.lionFnTopo = this.dummyLion;
            this.lionFnFlow = this.dummyLion;
            this.lion.loadCbs.set('detailscore', () => this.doLion());
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
     * and expect ShowDetails to be updated from data sent back from server.
     *
     * Note the difference in call to the WSS with requestDetails between a node
     * and a link - the handling is done in TopologyViewMessageHandler#RequestDetails.process()
     *
     * When multiple items are selected fabricate the ShowDetails here, and
     * present buttons that allow custom actions
     *
     * The WSS will call back asynchronously (see fn in ngOnInit())
     *
     * @param changes Simple Changes set of updates
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['selectedNodes']) {
            this.selectedNodes = changes['selectedNodes'].currentValue;
            let type: any;
            if (this.selectedNodes.length === 0) {
                // Selection has been cleared
                this.showDetails = <ShowDetails>{};
                return;
            } else if (this.selectedNodes.length > 1) {
                // Don't send message to WSS just form dialog here
                const propOrder: string[] = [];
                const propValues: Object = {};
                const propLabels: Object = {};
                let numHosts: number = 0;
                for (let i = 0; i < this.selectedNodes.length; i++) {
                    propOrder.push(i.toString());
                    propLabels[i.toString()] = i.toString();
                    propValues[i.toString()] = this.selectedNodes[i].id;
                    if (this.selectedNodes[i].hasOwnProperty('nodeType') &&
                        (<Host>this.selectedNodes[i]).nodeType === NodeType.HOST) {
                        numHosts++;
                    } else {
                        numHosts = -128; // Negate the whole thing so other buttons will not be shown
                    }
                }
                const buttons: string[] = [];
                if (numHosts === 2) {
                    buttons.push('createHostToHostFlow');
                } else if (numHosts > 2) {
                    buttons.push('createMultiSourceFlow');
                }
                buttons.push('relatedIntents');

                this.showDetails = <ShowDetails>{
                    buttons: buttons,
                    glyphId: undefined,
                    id: 'multiple',
                    navPath: undefined,
                    propLabels: propLabels,
                    propOrder: propOrder,
                    propValues: propValues,
                    title: this.lionFnTopo('title_selected_items')
                };
                this.log.debug('Details panel generated from multiple devices', this.showDetails);
                return;
            }

            // If only one thing has been selected then request details of that from the server
            const selectedNode = this.selectedNodes[0];
            if (selectedNode.hasOwnProperty('nodeType')) { // For Device, Host, SubRegion
                type = (<Host>selectedNode).nodeType;
                this.wss.sendEvent('requestDetails', {
                    id: selectedNode.id,
                    class: type,
                });
            } else if (selectedNode.hasOwnProperty('type')) { // Must be link
                const link: Link = <Link>selectedNode;
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
                this.log.warn('Unexpected type for selected element', selectedNode);
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
            case 'relatedIntents':
                return RELATEDINTENTS;
            case 'createHostToHostFlow':
                return CREATEHOSTTOHOSTFLOW;
            case 'createMultiSourceFlow':
                return CREATEMULTISOURCEFLOW;
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
     * When multiple hosts are selected other actions have to be accommodated
     *
     * @param path The path to navigate to
     * @param navPath The parameter name to use
     * @param selId the parameter value to use
     */
    navto(path: string): void {
        this.log.debug('navigate to', path, 'for',
            this.showDetails.navPath, '=', this.showDetails.id);

        const ids: string[] = [];
        Object.values(this.showDetails.propValues).forEach((v) => ids.push(v));
        if (path === 'relatedIntents' && this.showDetails.id === 'multiple') {
            this.wss.sendEvent('topo2RequestRelatedIntents', {
                'ids': ids,
                'hover': ''
            });

        } else if (path === 'create_h2h_flow' && this.showDetails.id === 'multiple') {
            this.wss.sendEvent('topo2AddHostIntent', {
                'one': ids[0],
                'two': ids[1],
                'ids': ids
            });

        } else if (path === 'create_msrc_flow' && this.showDetails.id === 'multiple') {
            // Should only happen when there are 3 or more ids
            this.wss.sendEvent('topo2AddMultiSourceIntent', {
                'src': ids.slice(0, ids.length - 1),
                'dst': ids[ids.length - 1],
                'ids': ids
            });

        } else if (this.showDetails.id) {
            let navPath = this.showDetails.navPath;
            if (navPath === 'device') {
                navPath = 'devId';
            }
            const queryPar: Params = {};
            queryPar[navPath] = this.showDetails.id;
            this.router.navigate([path], { queryParams: queryPar });
        }
    }

    /**
     * Read the LION bundle for Details panel and set up the lionFn
     */
    doLion() {
        this.lionFnTopo = this.lion.bundle('core.view.Topo');
        this.lionFnFlow = this.lion.bundle('core.view.Flow');
    }

}
