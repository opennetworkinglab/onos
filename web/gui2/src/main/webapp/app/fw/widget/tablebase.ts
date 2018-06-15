/*
 * Copyright 2015-present Open Networking Foundation
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
import { FnService } from '../util/fn.service';
import { LoadingService } from '../layer/loading.service';
import { LogService } from '../../log.service';
import { WebSocketService } from '../remote/websocket.service';

const REFRESH_INTERVAL = 2000;

/**
 * Base model of table view - implemented by Table components
 */
export interface TableBase {
    annots: TableAnnots;
    autoRefresh: boolean;
    autoRefreshTip: string;
    changedData: any;
    payloadParams: any;
    selId: string;
    sortParams: any;
    tableData: any[];
    toggleRefresh(): void;
    selectCallback(event: any, selRow: any): void;
    parentSelCb(event: any, selRow: any): void;
    sortCallback(): void;
    responseCallback(): void;
}

interface TableAnnots {
    noRowsMsg: string;
}

/**
 * A model of data returned in a TableResponse
 *
 * There is an interface extending from this one in the parent component
 */
export interface TableResponse {
    annots: any;
    // There will be other parts to the response depending on table type
    // Expect one called tag+'s' e.g. devices or apps
}

/**
 * ONOS GUI -- Widget -- Table Base class
 */
export class TableBaseImpl implements TableBase {
    // attributes from the interface
    public annots: TableAnnots;
    autoRefresh: boolean = true;
    autoRefreshTip: string = 'Toggle auto refresh'; // TODO: get LION string
    changedData: string[] = [];
    payloadParams: any;
    selId: string = undefined;
    sortParams: any;
    tableData: any[] = [];
    toggleRefresh; // Function
    selectCallback; // Function
    parentSelCb = null;
    sortCallback; // Function
    responseCallback; // Function

    private root: string;
    private req: string;
    private resp: string;
    private refreshPromise: any = null;
    private handlers: string[] = [];

    constructor(
        protected fs: FnService,
        protected ls: LoadingService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected tag: string,
        protected idKey: string = 'id',
        protected query: string = '',
        protected selCb = () => ({}) // Function
    ) {
        this.root = tag + 's';
        this.req = tag + 'DataRequest';
        this.resp = tag + 'DataResponse';

        this.sortCallback = this.requestTableData;
        this.selectCallback = this.rowSelectionCb;
        this.toggleRefresh = () => {
            this.autoRefresh = !this.autoRefresh;
            this.autoRefresh ? this.startRefresh() : this.stopRefresh();
        };
    }

    init() {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [this.resp, (data) => this.tableDataResponseCb(data)]
        ]));
        this.handlers.push(this.resp);

        this.annots = <TableAnnots>{
            noRowsMsg: ''
        };

        // Now send the WebSocket request and make it repeat every 2 seconds
        this.requestTableData();
        this.startRefresh();

        this.log.debug('TableBase initialized');
    }

    destroy() {
        this.wss.unbindHandlers(this.handlers);
        this.stopRefresh();
        this.ls.stop();
    }

    /**
     * A callback that executes when the table data that was requested
     * on WebSocketService arrives.
     *
     * Happens every 2 seconds
     */
    tableDataResponseCb(data: TableResponse) {
        this.ls.stop();

        const newTableData: any[] = Array.from(data[this.root]);
        this.annots.noRowsMsg = data.annots.no_rows_msg;

        // If the onResp() function is set then call it
        if (this.responseCallback) {
            this.responseCallback(data);
        }
        this.changedData = [];

        // checks if data changed for row flashing
        if (JSON.stringify(newTableData) !== JSON.stringify(this.tableData)) {
            this.log.debug('table data has changed');
            const oldTableData: any[] = this.tableData;
            this.tableData = [ ...newTableData ]; // ES6 spread syntax
            // only flash the row if the data already exists
            if (oldTableData.length > 0) {
                for (const idx in newTableData) {
                    if (!this.fs.containsObj(oldTableData, newTableData[idx])) {
                        this.changedData.push(newTableData[idx][this.idKey]);
                    }
                }
            }
        }
    }

    /**
     * Table Data Request
     */
    requestTableData() {
        const p = Object.assign({}, this.sortParams, this.payloadParams, this.query);

        // Allow it to sit in pending events
        if (this.wss.isConnected()) {
            if (this.fs.debugOn('table')) {
                this.log.debug('Table data REQUEST:', this.req, p);
            }
            this.wss.sendEvent(this.req, p);
            this.ls.start();
        }
    }

    /**
     * Row Selected
     */
    rowSelectionCb(event: any, selRow: any) {
        const selId: string = selRow[this.idKey];
        this.selId = (this.selId === selId) ? undefined : selId;
        if (this.parentSelCb) {
            this.log.debug('Parent called on Row', selId, 'selected');
            this.parentSelCb(event, selRow);
        }
    }

    /**
     * autoRefresh functions
     */
    startRefresh() {
        this.refreshPromise =
            setInterval(() => {
                if (!this.ls.waiting()) {
                    if (this.fs.debugOn('table')) {
                        this.log.debug('Refreshing ' + this.root + ' page');
                    }
                    this.requestTableData();
                }
            }, REFRESH_INTERVAL);
    }

    stopRefresh() {
        if (this.refreshPromise) {
            clearInterval(this.refreshPromise);
            this.refreshPromise = null;
        }
    }

    isChanged(id: string): boolean {
        return (this.fs.inArray(id, this.changedData) === -1) ? false : true;
    }
}
