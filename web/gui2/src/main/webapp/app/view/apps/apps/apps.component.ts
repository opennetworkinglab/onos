/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import {
    FnService,
    IconService,
    LionService,
    LogService,
    TableBaseImpl, TableResponse, SortDir,
    UrlFnService,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

const INSTALLED = 'INSTALLED';
const ACTIVE = 'ACTIVE';
const APPMGMTREQ = 'appManagementRequest';
const DETAILSREQ = 'appDetailsRequest';
const FILEUPLOADURL = 'upload';
const FILEDOWNLOADURL = 'download';
const ACTIVATEOPTION = '?activate=true';
const DRAGDROPMSG1 = 'Drag and drop one file at a time';
const DRAGDROPMSGEXT = 'Only files ending in .oar can be dropped';

/** Prefix to access the REST service for applications */
export const APPURLPREFIX = 'rs/applications/';
/** Suffix to access the icon of the application - gives back an image */
export const ICONURLSUFFIX = '/icon';

const downloadSuffix = '/download';
const dialogId = 'app-dialog';
const dialogOpts = {
    edge: 'right',
    width: 400,
};
const strongWarning = {
    'org.onosproject.drivers': true,
};
const propOrder = ['id', 'state', 'category', 'version', 'origin', 'role'];

/**
 * Model of the data returned through the Web Socket about apps.
 */
interface AppTableResponse extends TableResponse {
    apps: App[];
}

/**
 * Model of the data returned through Web Socket for a single App
 */
export interface App {
    category: string;
    desc: string;
    features: string[];
    icon: string;
    id: string;
    origin: string;
    permissions: string[];
    readme: string;
    required_apps: string[];
    role: string;
    state: string;
    title: string;
    url: string;
    version: string;
    _iconid_state: string;
}

export enum AppAction {
    NONE = 0,
    ACTIVATE = 1,
    DEACTIVATE = 2,
    UNINSTALL = 3,
}

/**
 * Model of the Control Button
 */
interface CtrlBtnState {
    installed: boolean;
    selection: string;
    active: boolean;
}

/**
 * ONOS GUI -- Apps View Component extends TableBaseImpl
 */
@Component({
  selector: 'onos-apps',
  templateUrl: './apps.component.html',
  styleUrls: [
    './apps.component.css', './apps.theme.css',
    '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css'
    ]
})
export class AppsComponent extends TableBaseImpl implements OnInit, OnDestroy {

    // deferred localization strings
    lionFn; // Function
    warnDeactivate: string;
    warnOwnRisk: string;
    ctrlBtnState: CtrlBtnState;
    appFile: any;

    uploadTip: string;
    activateTip: string;
    deactivateTip: string;
    uninstallTip: string;
    downloadTip: string;
    alertMsg: string;
    AppActionEnum: any = AppAction;
    appAction: AppAction = AppAction.NONE;
    confirmMsg: string = '';
    strongWarning: string = '';

    constructor(
        protected fs: FnService,
        private is: IconService,
        private lion: LionService,
        protected log: LogService,
        private ufs: UrlFnService,
        protected wss: WebSocketService,
        @Inject('Window') private window: Window,
        private httpClient: HttpClient
    ) {
        super(fs, log, wss, 'app');
        this.responseCallback = this.appResponseCb;
        this.parentSelCb =  this.rowSelection;
        // pre-populate sort so active apps are at the top of the list
        this.sortParams = {
            firstCol: 'state',
            firstDir: SortDir.desc,
            secondCol: 'title',
            secondDir: SortDir.asc,
        };
        // We want doLion() to be called only after the Lion
        // service is populated (from the WebSocket)
        // If lion is not ready we make do with a dummy function
        // As soon a lion gets loaded this function will be replaced with
        // the real thing
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('apps', () => this.doLion());
        } else {
            this.doLion();
        }

        this.ctrlBtnState = <CtrlBtnState>{
            installed: false,
            active: false
        };
    }

    /**
     * Initialize querying the WebSocket for App table details
     */
    ngOnInit() {
        this.init();
    }

    /**
     * Stop sending queries to WebSocket
     */
    ngOnDestroy() {
        this.lion.loadCbs.delete('apps');
        this.destroy();
        this.log.debug('AppComponent destroyed');
    }

    /**
     * The callback called when App data returns from WSS
     */
    appResponseCb(data: AppTableResponse) {
        this.log.debug('App response received for ', data.apps.length, 'apps');
    }

    /**
     * called when a row is selected - sets the state of control icons
     */
    rowSelection(event: any, selRow: any) {
        this.ctrlBtnState.installed = this.selId && selRow && selRow.state === INSTALLED;
        this.ctrlBtnState.active = this.selId && selRow && selRow.state === ACTIVE;
        this.ctrlBtnState.selection = this.selId;
        this.log.debug('Row ', this.selId, 'selected', this.ctrlBtnState);
    }


    /**
     * Perform one of the app actions - activate, deactivate or uninstall
     * Raises a dialog which calls back the dOk() below
     */
    confirmAction(action: AppAction): void {
        this.appAction = action;
        const appActionLc = (<string>AppAction[this.appAction]).toLowerCase();

        this.confirmMsg = this.lionFn(appActionLc) + ' ' + this.selId;
        if (strongWarning[this.selId]) {
            this.strongWarning = this.warnDeactivate + '\n' + this.warnOwnRisk;
        }

        this.log.debug('Initiating', this.appAction, 'of', this.selId);
    }

    /**
     * Callback when the Confirm dialog is shown and a choice is made
     */
    dOk(choice: boolean) {
        const appActionLc = (<string>AppAction[this.appAction]).toLowerCase();
        if (choice) {
            this.log.debug('Confirmed', appActionLc, 'on', this.selId);

            this.wss.sendEvent(APPMGMTREQ, {
                action: appActionLc,
                name: this.selId,
                sortCol: this.sortParams.firstCol,
                sortDir: SortDir[this.sortParams.firstDir],
            });
            if (this.appAction === AppAction.UNINSTALL) {
                this.selId = '';
            } else {
                this.wss.sendEvent(DETAILSREQ, { id: this.selId });
            }

        } else {
            this.log.debug('Cancelled', appActionLc, 'on', this.selId);
        }
        this.confirmMsg = '';
        this.strongWarning = '';
    }

    downloadApp() {
        if (this.ctrlBtnState.selection) {
            (<any>this.window).location = APPURLPREFIX + this.selId + '/' + FILEDOWNLOADURL;
        }
    }

    /**
     * When the file is selected this fires
     * It passes the file on to the server through a POST request
     * If there is an error its logged and raised to the user through Flash Component
     */
    fileEvent(event: any, activateImmediately?: boolean) {
        this.log.debug('File event for', event.target.files[0]);
        const formData = new FormData();
        formData.append('file', event.target.files[0]);
        let url = APPURLPREFIX + FILEUPLOADURL;
        if (activateImmediately) {
            url += ACTIVATEOPTION;
        }
        this.httpClient
            .post<any>(APPURLPREFIX + FILEUPLOADURL, formData)
            .subscribe(
                data => this.log.debug(data),
                err => {
                    this.log.warn(err.message);
                    this.alertMsg = err.message; // This will activate flash msg
                }
            );

    }

    /**
     * When the upload button is clicked pass this on to the file input (hidden)
     */
    triggerForm() {
        document.getElementById('uploadFile')
                .dispatchEvent(new MouseEvent('click'));
    }

    /**
     * Read the LION bundle for App and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.App');

        this.warnDeactivate = this.lionFn('dlg_warn_deactivate');
        this.warnOwnRisk = this.lionFn('dlg_warn_own_risk');

        this.uploadTip = this.lionFn('tt_ctl_upload');
        this.activateTip = this.lionFn('tt_ctl_activate');
        this.deactivateTip = this.lionFn('tt_ctl_deactivate');
        this.uninstallTip = this.lionFn('tt_ctl_uninstall');
        this.downloadTip = this.lionFn('tt_ctl_download');
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
        } else if (droppedFiles[0].name.slice(droppedFiles[0].name.length - 4) !== '.oar') {
            this.log.error(DRAGDROPMSGEXT, droppedFiles[0].name, 'rejected');
            this.alertMsg = DRAGDROPMSGEXT;
            return;
        }

        const fileEvent = {
            target: {
                files: droppedFiles
            }
        };
        this.fileEvent(fileEvent, true);
    }

    onDragOver(evt) {
        evt.preventDefault();
        evt.stopPropagation();
    }

    onDragLeave(evt) {
        evt.preventDefault();
        evt.stopPropagation();
    }

    deselectRow(event) {
        this.log.debug('Details panel close event');
        this.selId = event;
        this.ctrlBtnState = <CtrlBtnState>{
            installed: undefined,
            active: undefined
        };
    }

    getStateAsClass(value: string) {
        if (value === 'ACTIVE') {
            return 'active';
        } else if (value === 'INSTALLED') {
            return 'inactive';
        }
        return '';
    }
}
