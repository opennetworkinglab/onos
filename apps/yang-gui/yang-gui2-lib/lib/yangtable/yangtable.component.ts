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

import {Component, OnInit, OnDestroy, Inject} from '@angular/core';
import {WebSocketService, LogService, FnService, SortDir, TableBaseImpl} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { HttpClient } from '@angular/common/http';

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

/** Prefix to access the REST service for applications */
const YANGURLPREFIX = '../yang/models/';
const YANGMODELID = '?modelId=';
const DRAGDROPMSG1 = 'Drag and drop one file at a time';
const DRAGDROPMSGEXT = 'Only files ending in .yang, .zip or .jar can be loaded';

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
    alertMsg: string;

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected wss: WebSocketService,
        protected httpClient: HttpClient,
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
        document.getElementById('uploadYangFile')
            .dispatchEvent(new MouseEvent('click'));
    }

    /**
     * called when a row is selected - sets the state of control icons
     */
    rowSelectionParent(event: any, selRow: any) {
        this.selectedModel = selRow;
    }

    onDrop(event: DragEvent) {
        event.preventDefault();
        event.stopPropagation();

        const dt = event.dataTransfer;
        const droppedFiles = dt.files;

        this.log.debug(droppedFiles.length, 'File(s) dropped');
        if (droppedFiles.length !== 1) {
            this.log.error(DRAGDROPMSG1, droppedFiles.length, 'were dropped');
            this.alertMsg = DRAGDROPMSG1;
            return;
        }

        const fileEvent = {
            target: {
                files: droppedFiles
            }
        };
        this.fileEvent(fileEvent);
    }

    onDragOver(evt) {
        evt.preventDefault();
        evt.stopPropagation();
    }

    onDragLeave(evt) {
        evt.preventDefault();
        evt.stopPropagation();
    }

    /**
     * When the file is selected this fires
     * It passes the file on to the server through a POST request
     * If there is an error its logged and raised to the user through Flash Component
     */
    fileEvent(event: any): void {
        const file: File = event.target.files[0];
        const suffix: string = file.name.slice(file.name.lastIndexOf('.'));
        this.log.debug('File event for', file.name, file.type, file.size, suffix);
        if (suffix !== '.yang' && suffix !== '.jar' && suffix !== '.zip') {
            this.log.error(DRAGDROPMSGEXT, file.name, 'rejected');
            this.alertMsg = DRAGDROPMSGEXT;
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        this.httpClient
            .post<any>(YANGURLPREFIX + YANGMODELID + file.name, formData)
            .subscribe(
                data => this.log.debug(data),
                err => {
                    this.log.warn(err.error.message, err.status);
                    this.alertMsg = err.error.message; // This will activate flash msg
                }
            );
    }
}
