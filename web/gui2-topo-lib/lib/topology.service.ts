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
import {Injectable, SimpleChange} from '@angular/core';
import {
    LogService, WebSocketService,
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {Instance, InstanceComponent} from './panel/instance/instance.component';
import { BackgroundSvgComponent } from './layer/backgroundsvg/backgroundsvg.component';
import { ForceSvgComponent } from './layer/forcesvg/forcesvg.component';
import {
    ModelEventMemo,
    ModelEventType,
    Region
} from './layer/forcesvg/models';

/**
 * Model of the Intent to be displayed
 */
export interface Intent {
    appId: string;
    appName: string;
    key: string;
    type: string;
}

export interface RelatedIntent {
    ids: string[];
    hover: string;
}

/**
 * ONOS GUI -- Topology Service Module.
 */
@Injectable()
export class TopologyService {

    private handlers: string[] = [];
    private openListener: any;
    public instancesIndex: Map<string, number>;

    constructor(
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.instancesIndex = new Map();
        this.log.debug('TopologyService constructed');
    }

    /**
     * bind our event handlers to the web socket service, so that our
     * callbacks get invoked for incoming events
     */
    init(instance: InstanceComponent, background: BackgroundSvgComponent, force: ForceSvgComponent) {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['topo2AllInstances', (data) => {
                    this.log.debug('Instances updated through WSS as topo2AllInstances', data);
                    instance.ngOnChanges(
                        {'onosInstances': new SimpleChange({}, data.members, true)});

                    // Also generate an index locally of the instances
                    // needed so that devices can be coloured by instance
                    this.instancesIndex.clear();
                    (<Instance[]>data.members).forEach((inst, idx) => this.instancesIndex.set(inst.id, idx));
                    this.log.debug('Created local index of instances', this.instancesIndex);
                }
            ],
            ['topo2CurrentLayout', (data) => {
                    this.log.debug('Background Data updated from WSS as topo2CurrentLayout', data);
                    if (background) {
                        background.layoutData = data;
                    }
                }
            ],
            ['topo2CurrentRegion', (data) => {
                    force.regionData = data;
                    force.ngOnChanges({
                        'regionData' : new SimpleChange(<Region>{}, data, true)
                    });
                    this.log.debug('Region Data replaced from WSS as topo2CurrentRegion', force.regionData);
                }
            ],
            ['topo2PeerRegions', (data) => { this.log.warn('Add fn for topo2PeerRegions callback', data); } ],
            ['topo2UiModelEvent', (event) => {
                    // this.log.debug('Handling', event);
                    force.handleModelEvent(
                        <ModelEventType><unknown>(ModelEventType[event.type]), // Number based enum
                        <ModelEventMemo>(event.memo), // String based enum
                        event.subject, event.data);
                }
            ],
            ['showHighlights', (event) => {
                this.log.debug('Handling showHighlights', event);
                force.handleHighlights(event.devices, event.hosts, event.links, 5000);
            }]
            // topo2Highlights is handled by TrafficService
        ]));
        this.handlers.push('topo2AllInstances');
        this.handlers.push('topo2CurrentLayout');
        this.handlers.push('topo2CurrentRegion');
        this.handlers.push('topo2PeerRegions');
        this.handlers.push('topo2UiModelEvent');
        this.handlers.push('showHighlights');
        // this.handlers.push('topo2Highlights');

        // in case we fail over to a new server,
        // listen for wsock-open events
        this.openListener = this.wss.addOpenListener(() => this.wsOpen);

        // tell the server we are ready to receive topology events
        this.wss.sendEvent('topo2Start', {});
        this.log.debug('TopologyService initialized');
    }

    /**
     * tell the server we no longer wish to receive topology events
     */
    destroy() {
        this.wss.sendEvent('topo2Stop', {});
        this.wss.unbindHandlers(this.handlers);
        this.wss.removeOpenListener(this.openListener);
        this.openListener = null;
        this.log.debug('TopologyService destroyed');
    }


    wsOpen(host: string, url: string) {
        this.log.debug('topo2Event: WSopen - cluster node:', host, 'URL:', url);
        // tell the server we are ready to receive topo events
        this.wss.sendEvent('topo2Start', {});
    }

    /*
     * Result will be handled by showHighlights handler (set up in topology service)
     * which will call handleHighlights() in Force Component
     */
    setSelectedIntent(selectedIntent: Intent): void {
        this.log.debug('Selected intent changed to', selectedIntent);
        this.wss.sendEvent('selectIntent', selectedIntent);
    }

    selectRelatedIntent(ids: string[]): void {
        this.log.debug('Select next intent');
        this.wss.sendEvent('requestNextRelatedIntent', <RelatedIntent>{
            ids: ids,
            hover: undefined,
        });
    }

    /*
     * Tell the backend to stop sending highlights - any present will fade after 5 seconds
     * There is also a cancel traffic for Topo 2 in Traffic Service
     */
    cancelHighlights(): void {
        this.wss.sendEvent('cancelTraffic', {});
        this.log.debug('Highlights canceled');
    }
}
