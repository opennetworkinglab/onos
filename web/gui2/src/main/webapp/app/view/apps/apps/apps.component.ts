/*
 * Copyright 2015-present Open Networking Foundation
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
import { DialogService } from '../../../fw/layer/dialog.service';
import { FnService } from '../../../fw/util/fn.service';
import { IconService } from '../../../fw/svg/icon.service';
import { KeyService } from '../../../fw/util/key.service';
import { LionService } from '../../../fw/util/lion.service';
import { LoadingService } from '../../../fw/layer/loading.service';
import { LogService } from '../../../log.service';
import { TableBaseImpl, TableResponse, TableFilter, SortParams, SortDir } from '../../../fw/widget/table.base';
import { UrlFnService } from '../../../fw/remote/urlfn.service';
import { WebSocketService } from '../../../fw/remote/websocket.service';
import { TableFilterPipe } from '../../../fw/widget/tablefilter.pipe';

const INSTALLED = 'INSTALLED';
const ACTIVE = 'ACTIVE';
const appMgmtReq = 'appManagementRequest';
const topPdg = 60;
const panelWidth = 540;
const pName = 'application-details-panel';
const detailsReq = 'appDetailsRequest';
const detailsResp = 'appDetailsResponse';
const fileUploadUrl = 'applications/upload';
const activateOption = '?activate=true';

/** Prefix to access the REST service for applications */
export const APPURLPREFIX = '../../ui/rs/applications/'; // TODO: This is a hack to work off GUIv1 URL
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

/**
 * Model of the Control Button
 */
interface CtrlBtnState {
    installed: boolean;
    selection: string;
    active: boolean;
}

/**
 * ONOS GUI -- Apps View Component
 */
@Component({
  selector: 'onos-apps',
  templateUrl: './apps.component.html',
  styleUrls: [
    './apps.component.css', './apps.theme.css',
    '../../../fw/widget/table.css', '../../../fw/widget/table.theme.css'
    ]
})
export class AppsComponent extends TableBaseImpl implements OnInit, OnDestroy {

    // deferred localization strings
    lionFn; // Function
    warnDeactivate: string;
    warnOwnRisk: string;
    ctrlBtnState: CtrlBtnState;
    detailsPanel: any;
    appFile: any;
    activateImmediately = '';

    uploadTip: string;
    activateTip: string;
    deactivateTip: string;
    uninstallTip: string;
    downloadTip: string;

    constructor(
        protected fs: FnService,
        private ds: DialogService,
        private is: IconService,
        private ks: KeyService,
        private lion: LionService,
        protected ls: LoadingService,
        protected log: LogService,
        private ufs: UrlFnService,
        protected wss: WebSocketService,
        @Inject('Window') private window: Window,
    ) {
        super(fs, null, log, wss, 'app');
        this.responseCallback = this.appResponseCb;
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

    refreshCtrls() {
        let row;
        let rowIdx;
        if (this.ctrlBtnState.selection) {
            rowIdx = this.fs.find(this.selId, this.tableData);
            row = rowIdx >= 0 ? this.tableData[rowIdx] : null;

            this.ctrlBtnState.installed = row && row.state === INSTALLED;
            this.ctrlBtnState.active = row && row.state === ACTIVE;
        } else {
            this.ctrlBtnState.installed = false;
            this.ctrlBtnState.active = false;
        }
    }

    createConfirmationText(action, itemId) {
//        let content = this.ds.createDiv();
//        content.append('p').text(this.lionFn(action) + ' ' + itemId);
//        if (strongWarning[itemId]) {
//            content.append('p').html(
//                this.fs.sanitize(this.warnDeactivate) +
//                '<br>' +
//                this.fs.sanitize(this.warnOwnRisk)
//            ).classed('strong', true);
//        }
//        return content;
    }

    confirmAction(action): void {
        const itemId = this.selId;
        const spar = this.sortParams;

        function dOk() {
            this.log.debug('Initiating', action, 'of', itemId);
            this.wss.sendEvent(appMgmtReq, {
                action: action,
                name: itemId,
                sortCol: spar.firstCol,
                sortDir: spar.firstDir,
            });
            if (action === 'uninstall') {
                this.detailsPanel.hide();
            } else {
                this.wss.sendEvent(detailsReq, { id: itemId });
            }
        }

        function dCancel() {
            this.log.debug('Canceling', action, 'of', itemId);
        }

//        this.ds.openDialog(dialogId, dialogOpts)
//            .setTitle(this.lionFn('dlg_confirm_action'))
//            .addContent(this.createConfirmationText(action, itemId))
//            .addOk(dOk)
//            .addCancel(dCancel)
//            .bindKeys();
    }

    appAction(action) {
        if (this.ctrlBtnState.selection) {
            this.confirmAction(action);
        }
    }

    downloadApp() {
        if (this.ctrlBtnState.selection) {
            (<any>this.window).location = APPURLPREFIX + this.selId + ICONURLSUFFIX;
        }
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

    appDropped() {
        this.activateImmediately = activateOption;
//        $scope.$emit('FileChanged'); // TODO: Implement this
        this.appFile = null;
    }
}
