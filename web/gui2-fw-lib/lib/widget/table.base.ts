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
import { FnService } from '../util/fn.service';
import { LogService } from '../log.service';
import { WebSocketService } from '../remote/websocket.service';
import { Observable, of } from 'rxjs';

const REFRESH_INTERVAL = 2000;
const SEARCH_REGEX = '\\W';

/**
 * Model of table annotations within this table base class
 */
export interface TableAnnots {
    noRowsMsg: string;
}

/**
 * A model of data returned from Web Socket in a TableResponse
 *
 * There is an interface extending from this one in the parent component
 */
export interface TableResponse {
    annots: any;
    // There will be other parts to the response depending on table type
    // Expect one called tag+'s' e.g. devices or apps
}

/**
 * A criteria for filtering the tableData
 */
export interface TableFilter {
    queryStr: string;
    queryBy: string;
    sortBy: string;
}

/**
 * Enumerated values for the sort dir
 */
export enum SortDir {
    asc = 'asc', desc = 'desc'
}

/**
 * A structure to format sort params for table
 * This is sent to WebSocket as part of table request
 */
export interface SortParams {
    firstCol: string;
    firstDir: SortDir;
    secondCol: string;
    secondDir: SortDir;
}

export interface PayloadParams {
    devId: string;
}


/**
 * ONOS GUI -- Widget -- Table Base class
 */
export abstract class TableBaseImpl {
    // attributes from the interface
    public annots: TableAnnots;
    protected changedData: string[] = [];
    protected payloadParams: PayloadParams;
    protected sortParams: SortParams;
    public selectCallback; // Function
    protected parentSelCb = null;
    protected responseCallback; // Function
    public loadingIconShown: boolean = false;
    selId: string = undefined;
    tableData: any[] = [];
    tableDataFilter: TableFilter;
    toggleRefresh; // Function
    autoRefresh: boolean = true;
    autoRefreshTip: string = 'Toggle auto refresh'; // TODO: get LION string

    readonly root: string;
    readonly req: string;
    readonly resp: string;
    private refreshPromise: any = null;
    private handlers: string[] = [];

    protected constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        protected tag: string,
        protected idKey: string = 'id',
        protected selCb = () => ({}) // Function
    ) {
        this.root = tag + 's';
        this.req = tag + 'DataRequest';
        this.resp = tag + 'DataResponse';

        this.selectCallback = this.rowSelectionCb;
        this.toggleRefresh = () => {
            this.autoRefresh = !this.autoRefresh;
            this.autoRefresh ? this.startRefresh() : this.stopRefresh();
        };

        // Mapped to the search and searchBy inputs in template
        // Changes are handled through TableFilterPipe
        this.tableDataFilter = <TableFilter>{
            queryStr: '',
            queryBy: '$',
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
        this.log.debug('TableBase initialized. Calling ', this.req,
            'every', REFRESH_INTERVAL, 'ms');
    }

    destroy() {
        this.wss.unbindHandlers(this.handlers);
        this.stopRefresh();
        this.loadingIconShown = false;
    }

    /**
     * A callback that executes when the table data that was requested
     * on WebSocketService arrives.
     *
     * Happens every 2 seconds
     */
    tableDataResponseCb(data: TableResponse) {
        this.loadingIconShown = false;

        const newTableData: any[] = Array.from(data[this.root]);
        this.annots.noRowsMsg = data.annots.no_rows_msg;

        // If the parents onResp() function is set then call it
        if (this.responseCallback) {
            this.responseCallback(data);
        }
        this.changedData = [];

        // checks if data changed for row flashing
        if (JSON.stringify(newTableData) !== JSON.stringify(this.tableData)) {
            this.log.debug('table data has changed');
            const oldTableData: any[] = this.tableData;
            this.tableData = [...newTableData]; // ES6 spread syntax
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
     * Pass in sort parameters and the set will be returned sorted
     * In the old GUI there was also a query parameter, but this was not
     * implemented on the server end
     */
    requestTableData() {
        const p = (<any>Object).assign({}, this.sortParams, this.payloadParams);

        // Allow it to sit in pending events
        if (this.wss.isConnected()) {
            if (this.fs.debugOn('table')) {
                this.log.debug('Table data REQUEST:', this.req, p);
            }
            this.wss.sendEvent(this.req, p);
            this.loadingIconShown = true;
        }
    }

    /**
     * Row Selected
     */
    rowSelectionCb(event: any, selRow: any): void {
        const selId: string = selRow[this.idKey];
        this.selId = (this.selId === selId) ? undefined : selId;
        this.log.debug('Row', selId, 'selected');
        if (this.parentSelCb) {
            this.parentSelCb(event, selRow);
        }
    }

    /**
     * autoRefresh functions
     */
    startRefresh() {
        this.refreshPromise =
            setInterval(() => {
                if (!this.loadingIconShown) {
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

    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }

    /**
     * Change the sort order of the data returned
     *
     * sortParams are passed to the server by WebSocket and the data is
     * returned sorted
     *
     * This is usually assigned to the (click) event on a column, and the column
     * name passed in e.g. (click)="onSort('origin')
     * If the column that is passed in is already the firstCol, then reverse its direction
     * If a new column is passed in, then make the existing col the 2nd sort order
     */
    onSort(colName: string) {
        if (this.sortParams.firstCol === colName) {
            if (this.sortParams.firstDir === SortDir.desc) {
                this.sortParams.firstDir = SortDir.asc;
                return;
            } else {
                this.sortParams.firstDir = SortDir.desc;
                return;
            }
        } else {
            this.sortParams.secondCol = this.sortParams.firstCol;
            this.sortParams.secondDir = this.sortParams.firstDir;
            this.sortParams.firstCol = colName;
            this.sortParams.firstDir = SortDir.desc;
        }
        this.log.debug('Sort params', this.sortParams);
        this.requestTableData();
    }

    sortIcon(column: string): string {
        if (this.sortParams.firstCol === column) {
            if (this.sortParams.firstDir === SortDir.asc) {
                return 'upArrow';
            } else {
                return 'downArrow';
            }
        } else {
            return '';
        }
    }

    /**
     * De-selects the row
     */
    deselectRow(event) {
        this.log.debug('Details panel close event');
        this.selId = event;
    }
}
