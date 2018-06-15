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
import { Component, OnInit, OnDestroy } from '@angular/core';
import { DialogService } from '../../fw/layer/dialog.service';
import { FnService } from '../../fw/util/fn.service';
import { IconService } from '../../fw/svg/icon.service';
import { KeyService } from '../../fw/util/key.service';
import { LionService } from '../../fw/util/lion.service';
import { LoadingService } from '../../fw/layer/loading.service';
import { LogService } from '../../log.service';
import { PanelService } from '../../fw/layer/panel.service';
import { TableBaseImpl, TableResponse } from '../../fw/widget/tablebase';
import { UrlFnService } from '../../fw/remote/urlfn.service';
import { WebSocketService } from '../../fw/remote/websocket.service';

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
const appUrlPrefix = 'rs/applications/';
const iconUrlSuffix = '/icon';
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

interface AppTableResponse extends TableResponse {
    apps: Apps[];
}

interface Apps {
    category: string;
    desc: string;
    features: string;
    icon: string;
    id: string;
    origin: string;
    permissions: string;
    readme: string;
    required_apps: string;
    role: string;
    state: string;
    title: string;
    url: string;
    version: string;
    _iconid_state: string;
}

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
    '../../fw/widget/table.css', '../../fw/widget/table-theme.css'
    ]
})
export class AppsComponent extends TableBaseImpl implements OnInit, OnDestroy {

    // deferred localization strings
    lionFn; // Function
    warnDeactivate: string;
    warnOwnRisk: string;
    friendlyProps: string[];
    ctrlBtnState: CtrlBtnState;
    detailsPanel: any;

    constructor(
        protected fs: FnService,
        private ds: DialogService,
        private is: IconService,
        private ks: KeyService,
        private lion: LionService,
        protected ls: LoadingService,
        protected log: LogService,
        private ps: PanelService,
        private ufs: UrlFnService,
        protected wss: WebSocketService,
        private window: Window,
    ) {
        super(fs, null, log, wss, 'app');
        this.responseCallback = this.appResponseCb;
        this.sortParams = {
            firstCol: 'state',
            firstDir: 'desc',
            secondCol: 'title',
            secondDir: 'asc',
        };
        // We want doLion() to be called only after the Lion service is populated (from the WebSocket)
        this.lion.loadCb = (() => this.doLion());
        this.ctrlBtnState = <CtrlBtnState>{
            installed: false,
            active: false
        };
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
        } else {
            this.doLion();
        }
    }

    ngOnInit() {
        this.init();
        this.log.debug('AppComponent initialized');
    }

    ngOnDestroy() {
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
                sortCol: spar.sortCol,
                sortDir: spar.sortDir,
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
            (<any>this.window).location = appUrlPrefix + this.selId + downloadSuffix;
        }
    }

    /**
     * Read the LION bundle for App - this should replace the dummyLion implementation
     * of lionFn with a function from the LION Service
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.App');

        this.warnDeactivate = this.lionFn('dlg_warn_deactivate');
        this.warnOwnRisk = this.lionFn('dlg_warn_own_risk');

        this.friendlyProps = [
            this.lionFn('app_id'), this.lionFn('state'),
            this.lionFn('category'), this.lionFn('version'),
            this.lionFn('origin'), this.lionFn('role'),
        ];
    }

    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
