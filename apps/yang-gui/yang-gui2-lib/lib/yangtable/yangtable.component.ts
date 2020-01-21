/*
 * Copyright 2020-present Open Networking Foundation
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

import {Component, OnInit, OnDestroy} from '@angular/core';
import {WebSocketService, LogService, FnService, SortDir, TableBaseImpl, TableResponse} from 'gui2-fw-lib/public_api';

/**
 * Model of the response from the WebSocket
 */
export interface YangModel {
    id: string;
    revision: string;
    modelId: string;
}

export interface YangModelDataResponse {
    yangModels: YangModel[];
}

@Component({
    selector: 'onos-yangtable',
    templateUrl: './yangtable.component.html',
    styleUrls: [
        './yangtable.component.scss', 'yangtable.theme.scss',
        '../../../../../web/gui2-fw-lib/lib/widget/table.css', '../../../../../web/gui2-fw-lib/lib/widget/table.theme.css'
    ]
})
export class YangTableComponent extends TableBaseImpl implements OnInit, OnDestroy {
    selectedModel: YangModel = undefined;

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'yangModel');
        this.log.debug('YangTableComponent constructed');
        this.parentSelCb =  this.rowSelectionParent;

        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'revision',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('YangTableComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('YangTableComponent destroyed');
    }

    /**
     * When the upload button is clicked pass this on to the file input (hidden)
     */
    triggerForm() {
        document.getElementById('uploadFile')
            .dispatchEvent(new MouseEvent('click'));
    }

    /**
     * called when a row is selected - sets the state of control icons
     */
    rowSelectionParent(event: any, selRow: any) {
        this.selectedModel = selRow;
    }
}
