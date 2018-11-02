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
import { Injectable } from '@angular/core';
import {
    LogService, WebSocketService,
} from 'gui2-fw-lib';
import { InstanceComponent } from './panel/instance/instance.component';
import { BackgroundSvgComponent } from './layer/backgroundsvg/backgroundsvg.component';
import { ForceSvgComponent } from './layer/forcesvg/forcesvg.component';

/**
 * ONOS GUI -- Topology Service Module.
 */
@Injectable()
export class TopologyService {

    private handlers: string[] = [];
    private openListener: any;

    constructor(
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.log.debug('TopologyService constructed');
    }

    /**
     * bind our event handlers to the web socket service, so that our
     * callbacks get invoked for incoming events
     */
    init(instance: InstanceComponent, background: BackgroundSvgComponent, force: ForceSvgComponent) {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['topo2AllInstances', (data) => {
                    this.log.warn('Add fn for topo2AllInstances callback', data);
                    instance.onosInstances = data.members;
                }
            ],
            ['topo2CurrentLayout', (data) => {
                    this.log.warn('Add fn for topo2CurrentLayout callback', data);
                    background.layoutData = data;
                }
            ],
            ['topo2CurrentRegion', (data) => {
                    this.log.warn('Add fn for topo2CurrentRegion callback', data);
                    force.regionData = data;
                }
            ],
            ['topo2PeerRegions', (data) => { this.log.warn('Add fn for topo2PeerRegions callback', data); } ],
            ['topo2UiModelEvent', (data) => { this.log.warn('Add fn for topo2UiModelEvent callback', data); } ],
            ['topo2Highlights', (data) => { this.log.warn('Add fn for topo2Highlights callback', data); } ],
        ]));
        this.handlers.push('topo2AllInstances');
        this.handlers.push('topo2CurrentLayout');
        this.handlers.push('topo2CurrentRegion');
        this.handlers.push('topo2PeerRegions');
        this.handlers.push('topo2UiModelEvent');
        this.handlers.push('topo2Highlights');

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
}
