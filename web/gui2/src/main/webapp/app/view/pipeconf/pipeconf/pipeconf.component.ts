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

import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {
    FnService,
    LogService, SortDir,
    TableBaseImpl,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {ActivatedRoute, Router} from '@angular/router';

const pipeconfReq = 'pipeconfRequest';
const pipeconfResp = 'pipeConfResponse';

export interface PipeconfHeader {
    id: string;
    behaviors: string[];
    extensions: string[];
}

export interface ActionParam {
    name: string;
    bitWidth: number;
}

export interface PipeconfAction {
    name: string;
    params: ActionParam[];
}

export interface MatchFields {
    matchType: string;
    bitWidth: number;
    field: string;
}

export interface PipeconfTable {
    name: string;
    maxSize: number;
    hasCounters: boolean;
    supportAging: boolean;
    matchFields: MatchFields[];
    actions: string[];
}

export interface PipelineModel {
    actions: PipeconfAction[];
    tables: PipeconfTable[];
}

export interface PipeconfData {
    pipeconf: PipeconfHeader;
    pipelineModel: PipelineModel;
}


export interface TableStat {
    table: string;
    active: number;
    haslookedup: boolean;
    lookedup: number;
    matched: number;
    hasmaxsize: boolean;
    maxsize: number;
}

export interface TableStats {
    tableStats: TableStat[];
}

/**
 * ONOS GUI -- Pipeconf View Component
 */
@Component({
    selector: 'onos-pipeconf',
    templateUrl: './pipeconf.component.html',
    styleUrls: ['./pipeconf.component.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class PipeconfComponent extends TableBaseImpl implements OnInit, OnDestroy {
    devId: string;
    pipeconfData: PipeconfData;
    selectedTable: PipeconfTable;
    parentSelCb = this.updateSelected; // Func

    // TODO: Update for LION
    flowTip = 'Show flow view for selected device';
    portTip = 'Show port view for selected device';
    groupTip = 'Show group view for selected device';
    meterTip = 'Show meter view for selected device';
    pipeconfTip = 'Show pipeconf view for selected device';
    na = 'N/A';

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ar: ActivatedRoute,
        protected router: Router,
        protected wss: WebSocketService,
        private ref: ChangeDetectorRef,
    ) {
        super(fs, log, wss, 'tableStat', 'table');
        this.ar.queryParams.subscribe(params => {
            this.devId = params['devId'];

        });

        this.payloadParams = {
            devId: this.devId
        };

        this.responseCallback = this.tableStatsResponseCb;

        this.sortParams = {
            firstCol: 'table',
            firstDir: SortDir.desc,
            secondCol: 'active',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        // Also make a once off call to get the Pipeconf Static information
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [pipeconfResp, (data) => {
                this.pipeconfData = data;
                this.ref.markForCheck();
                this.log.debug('Pipeconf static data received', this.pipeconfData);
            }]
        ]));

        const p = (<any>Object).assign({}, this.sortParams, this.payloadParams);

        // Allow it to sit in pending events
        if (this.wss.isConnected()) {
            this.wss.sendEvent(pipeconfReq, p);
        }

        this.init();

        this.log.debug('PipeconfComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.wss.unbindHandlers([pipeconfResp]);

        this.log.debug('PipeconfComponent destroyed');
    }

    tableStatsResponseCb(newTableData: TableStats) {
        // checks if data changed for row flashing
    }

    /**
     * There's nothing to navigate down further to for Pipeconf - instead we can
     * navigate for the Device that was passed in
     * @param path
     */
    navto(path) {
        this.log.debug('navigate to', path);
        this.router.navigate([path], { queryParams: { devId: this.devId } });
    }

    pipeconfModelTable(table: string): PipeconfTable {
        if (this.pipeconfData === undefined) {
            return; // Not loaded yet
        }
        for (const t of this.pipeconfData.pipelineModel.tables) {
            if (t.name === table) {
                return t;
            }
        }
        this.log.warn('Could not find table', table, 'in PipelineModel');
    }

    updateSelected(event: any, selRow: any): void {
        if (this.selId === undefined) {
            this.selectedTable = undefined;
        } else {
            this.selectedTable = this.pipeconfModelTable(this.selId);
        }
    }
}
