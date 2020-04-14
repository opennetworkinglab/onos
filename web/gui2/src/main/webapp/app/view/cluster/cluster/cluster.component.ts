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
import {Component, OnDestroy, OnInit} from '@angular/core';
import {
    FnService,
    LogService,
    WebSocketService,
    LionService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * Model of the response from WebSocket
 */
interface ClusterTableResponse extends TableResponse {
    clusters: Cluster[];
}

/**
 * Model of the cluster returned from the WebSocket
 */
interface Cluster {
    _iconid_state: string;
    _iconid_started: string;
    active: string;
    started: string;
    nodeId: string;
    ipAddress: string;
    tcpPort: string;
    lastUpdated: string;
}

/**
 * ONOS GUI -- Cluster View Component
 */
@Component({
  selector: 'onos-cluster',
  templateUrl: './cluster.component.html',
  styleUrls: ['./cluster.component.css', './cluster.theme.css',
      '../../../../../../../../gui2-fw-lib/lib/widget/table.css',
      '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})

export class ClusterComponent extends TableBaseImpl implements OnInit, OnDestroy {

    lionFn; // Function

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected lion: LionService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'cluster');
        this.responseCallback = this.clusterResponseCb;

        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'ip',
            secondDir: SortDir.asc,
        };

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('cluster', () => this.doLion());
        } else {
            this.doLion();
        }
    }

    ngOnInit() {
        this.init();
        this.log.debug('ClusterComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('ClusterComponent destroyed');
    }

    clusterResponseCb(data: ClusterTableResponse) {
        this.log.debug('Cluster response received for ', data.clusters.length, 'cluster');
    }

    doLion() {
        this.lionFn = this.lion.bundle('core.view.Cluster');

    }
}
