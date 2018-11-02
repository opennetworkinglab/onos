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
import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import * as d3 from 'd3';
import {
    FnService,
    KeysService, KeysToken,
    LogService, PrefsService,
    SvgUtilService, WebSocketService, Zoomer, ZoomOpts, ZoomService
} from 'gui2-fw-lib';
import { InstanceComponent } from '../panel/instance/instance.component';
import { SummaryComponent } from '../panel/summary/summary.component';
import { DetailsComponent } from '../panel/details/details.component';
import { BackgroundSvgComponent } from '../layer/backgroundsvg/backgroundsvg.component';
import { ForceSvgComponent } from '../layer/forcesvg/forcesvg.component';
import { TopologyService } from '../topology.service';

/**
 * ONOS GUI Topology View
 *
 * This Topology View component is the top level component in a hierarchy that
 * comprises the whole Topology View
 *
 * There are three main parts (panels, graphical and breadcrumbs)
 * The panel hierarchy
 * |-- Instances Panel (shows ONOS instances)
 * |-- Summary Panel (summary of ONOS)
 * |-- Toolbar Panel (the toolbar)
 * |-- Details Panel (when a node is selected in the Force graphical view (see below))
 *
 * The graphical hierarchy contains
 * Topology (this)
 *  |-- No Devices Connected (only of there are no nodes to show)
 *  |-- Zoom Layer (everything beneath this can be zoomed and panned)
 *      |-- Background (container for any backgrounds - can be toggled on and off)
 *          |-- Map
 *      |-- Forces (all of the nodes and links laid out by a d3.force simulation)
 *
 * The breadcrumbs
 * |-- Breadcrumb (in region view a way of navigating back up through regions)
 */
@Component({
  selector: 'onos-topology',
  templateUrl: './topology.component.html',
  styleUrls: ['./topology.component.css']
})
export class TopologyComponent implements OnInit, OnDestroy {
    // These are references to the components inserted in the template
    @ViewChild(InstanceComponent) instance: InstanceComponent;
    @ViewChild(SummaryComponent) summary: SummaryComponent;
    @ViewChild(DetailsComponent) details: DetailsComponent;
    @ViewChild(BackgroundSvgComponent) background: BackgroundSvgComponent;
    @ViewChild(ForceSvgComponent) force: ForceSvgComponent;

    flashMsg: string = '';
    prefsState = {};
    hostLabelIdx: number = 1;

    zoomer: Zoomer;
    zoomEventListeners: any[];

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected ks: KeysService,
        protected sus: SvgUtilService,
        protected ps: PrefsService,
        protected wss: WebSocketService,
        protected zs: ZoomService,
        protected ts: TopologyService,
    ) {

        this.log.debug('Topology component constructed');
    }

    ngOnInit() {
        this.bindCommands();
        this.zoomer = this.createZoomer(<ZoomOpts>{
            svg: d3.select('svg#topo2'),
            zoomLayer: d3.select('g#topo-zoomlayer'),
            zoomEnabled: () => true,
            zoomMin: 0.25,
            zoomMax: 10.0,
            zoomCallback: (() => { return; })
        });
        this.zoomEventListeners = [];
        // The components from the template are handed over to TopologyService here
        // so that WebSocket responses can be passed back in to them
        // The handling of the WebSocket call is delegated out to the Topology
        // Service just to compartmentalize things a bit
        this.ts.init(this.instance, this.background, this.force);
        this.log.debug('Topology component initialized');
    }

    ngOnDestroy() {
        this.ts.destroy();
        this.log.debug('Topology component destroyed');
    }

    actionMap() {
        return {
            L: [() => {this.cycleDeviceLabels(); }, 'Cycle device labels'],
            B: [(token) => {this.toggleBackground(token); }, 'Toggle background'],
            D: [(token) => {this.toggleDetails(token); }, 'Toggle details panel'],
            I: [(token) => {this.toggleInstancePanel(token); }, 'Toggle ONOS Instance Panel'],
            O: [() => {this.toggleSummary(); }, 'Toggle the Summary Panel'],
            R: [() => {this.resetZoom(); }, 'Reset pan / zoom'],
            'shift-Z': [() => {this.panAndZoom([0, 0], this.zoomer.scale() * 2); }, 'Zoom x2'],
            'alt-Z': [() => {this.panAndZoom([0, 0], this.zoomer.scale() / 2); }, 'Zoom x0.5'],
            P: [(token) => {this.togglePorts(token); }, 'Toggle Port Highlighting'],
            E: [() => {this.equalizeMasters(); }, 'Equalize mastership roles'],
            X: [() => {this.resetNodeLocation(); }, 'Reset Node Location'],
            U: [() => {this.unpinNode(); }, 'Unpin node (mouse over)'],
            H: [() => {this.toggleHosts(); }, 'Toggle host visibility'],
            M: [() => {this.toggleOfflineDevices(); }, 'Toggle offline visibility'],
            dot: [() => {this.toggleToolbar(); }, 'Toggle Toolbar'],
            'shift-L': [() => {this.cycleHostLabels(); }, 'Cycle host labels'],

            // -- instance color palette debug
            9: () => {
                this.sus.cat7().testCard(d3.select('svg#topo2'));
            },

            esc: this.handleEscape,

            // TODO update after adding in Background Service
            // topology overlay selections
            // F1: function () { t2tbs.fnKey(0); },
            // F2: function () { t2tbs.fnKey(1); },
            // F3: function () { t2tbs.fnKey(2); },
            // F4: function () { t2tbs.fnKey(3); },
            // F5: function () { t2tbs.fnKey(4); },
            //
            // _keyListener: t2tbs.keyListener.bind(t2tbs),

            _helpFormat: [
                ['I', 'O', 'D', 'H', 'M', 'P', 'dash', 'B'],
                ['X', 'Z', 'N', 'L', 'shift-L', 'U', 'R', 'E', 'dot'],
                [], // this column reserved for overlay actions
            ],
        };
    }


    bindCommands(additional?: any) {

        const am = this.actionMap();
        const add = this.fs.isO(additional);

        // TODO: Reimplement when we have a use case
        // if (add) {
        //     _.each(add, function (value, key) {
        //         // filter out meta properties (e.g. _keyOrder)
        //         if (!(_.startsWith(key, '_'))) {
        //             // don't allow re-definition of existing key bindings
        //             if (am[key]) {
        //                 this.log.warn('keybind: ' + key + ' already exists');
        //             } else {
        //                 am[key] = [value.cb, value.tt];
        //             }
        //         }
        //     });
        // }

        this.ks.keyBindings(am);

        this.ks.gestureNotes([
            ['click', 'Select the item and show details'],
            ['shift-click', 'Toggle selection state'],
            ['drag', 'Reposition (and pin) device / host'],
            ['cmd-scroll', 'Zoom in / out'],
            ['cmd-drag', 'Pan'],
        ]);
    }

    handleEscape() {

        if (false) {
            // TODO: Cancel show mastership
            // TODO: Cancel Active overlay
            // TODO: Reinstate with components
        } else {
            this.log.debug('Handling escape');
            // } else if (t2rs.deselectAllNodes()) {
            //     // else if we have node selections, deselect them all
            //     // (work already done)
            // } else if (t2rs.deselectLink()) {
            //     // else if we have a link selection, deselect it
            //     // (work already done)
            // } else if (t2is.isVisible()) {
            //     // If the instance panel is visible, close it
            //     t2is.toggle();
            // } else if (t2sp.isVisible()) {
            //     // If the summary panel is visible, close it
            //     t2sp.toggle();
        }
    }



    updatePrefsState(what, b) {
        this.prefsState[what] = b ? 1 : 0;
        this.ps.setPrefs('topo2_prefs', this.prefsState);
    }

    deviceLabelFlashMessage(index) {
        switch (index) {
            case 0: return 'Hide device labels';
            case 1: return 'Show friendly device labels';
            case 2: return 'Show device ID labels';
        }
    }

    hostLabelFlashMessage(index) {
        switch (index) {
            case 0: return 'Hide host labels';
            case 1: return 'Show friendly host labels';
            case 2: return 'Show host IP labels';
            case 3: return 'Show host MAC Address labels';
        }
    }

    protected cycleDeviceLabels() {
        this.log.debug('Cycling device labels');
        // TODO: Reinstate with components
        // let deviceLabelIndex = t2ps.get('dlbls') + 1;
        // let newDeviceLabelIndex = deviceLabelIndex % 3;
        //
        // t2ps.set('dlbls', newDeviceLabelIndex);
        // t2fs.updateNodes();
        // flash.flash(deviceLabelFlashMessage(newDeviceLabelIndex));
    }

    protected cycleHostLabels() {
        const hostLabelIndex = this.hostLabelIdx + 1;
        this.hostLabelIdx = hostLabelIndex % 4;
        this.flashMsg = this.hostLabelFlashMessage(this.hostLabelIdx);
        this.log.debug('Cycling host labels');
        // TODO: Reinstate with components
        // t2ps.set('hlbls', newHostLabelIndex);
        // t2fs.updateNodes();
    }

    protected toggleBackground(token: KeysToken) {
        this.flashMsg = 'Toggling background';
        this.log.debug('Toggling background', token);
        // TODO: Reinstate with components
        // t2bgs.toggle(x);
    }

    protected toggleDetails(token: KeysToken) {
        this.flashMsg = 'Toggling details';
        this.details.togglePanel(() => {});
        this.log.debug('Toggling details', token);
    }

    protected toggleInstancePanel(token: KeysToken) {
        this.flashMsg = 'Toggling instances';
        this.instance.togglePanel(() => {});
        this.log.debug('Toggling instances', token);
        // TODO: Reinstate with components
        // this.updatePrefsState('insts', t2is.toggle(x));
    }

    protected toggleSummary() {
        this.flashMsg = 'Toggling summary';
        this.summary.togglePanel(() => {});
    }

    protected resetZoom() {
        this.zoomer.reset();
        this.log.debug('resetting zoom');
        // TODO: Reinstate with components
        // t2bgs.resetZoom();
        // flash.flash('Pan and zoom reset');
    }

    protected togglePorts(token: KeysToken) {
        this.log.debug('Toggling ports');
        // TODO: Reinstate with components
        // this.updatePrefsState('porthl', t2vs.togglePortHighlights(x));
        // t2fs.updateLinks();
    }

    protected equalizeMasters() {
        this.wss.sendEvent('equalizeMasters', null);

        this.log.debug('equalizing masters');
        // TODO: Reinstate with components
        // flash.flash('Equalizing master roles');
    }

    protected resetNodeLocation() {
        this.log.debug('resetting node location');
        // TODO: Reinstate with components
        // t2fs.resetNodeLocation();
        // flash.flash('Reset node locations');
    }

    protected unpinNode() {
        this.log.debug('unpinning node');
        // TODO: Reinstate with components
        // t2fs.unpin();
        // flash.flash('Unpin node');
    }

    protected toggleToolbar() {
        this.log.debug('toggling toolbar');
        // TODO: Reinstate with components
        // t2tbs.toggle();
    }

    protected actionedFlashed(action, message) {
        this.log.debug('action flashed');
        // TODO: Reinstate with components
        // this.flash.flash(action + ' ' + message);
    }

    protected toggleHosts() {
        // this.flashMsg = on ? 'Show': 'Hide', 'Hosts';
        this.log.debug('toggling hosts');
        // TODO: Reinstate with components
        // let on = t2rs.toggleHosts();
        // this.actionedFlashed(on ? 'Show': 'Hide', 'Hosts');
    }

    protected toggleOfflineDevices() {
        this.log.debug('toggling offline devices');
        // TODO: Reinstate with components
        // let on = t2rs.toggleOfflineDevices();
        // this.actionedFlashed(on ? 'Show': 'Hide', 'offline devices');
    }

    protected notValid(what) {
        this.log.warn('topo.js getActionEntry(): Not a valid ' + what);
    }

    getActionEntry(key) {
        let entry;

        if (!key) {
            this.notValid('key');
            return null;
        }

        entry = this.actionMap()[key];

        if (!entry) {
            this.notValid('actionMap (' + key + ') entry');
            return null;
        }
        return this.fs.isA(entry) || [entry, ''];
    }



    protected createZoomer(options: ZoomOpts) {
        // need to wrap the original zoom callback to extend its behavior
        const origCallback = this.fs.isF(options.zoomCallback) ? options.zoomCallback : () => {};

        options.zoomCallback = () => {
            origCallback([0, 0], 1);

            this.zoomEventListeners.forEach((ev) => ev(this.zoomer));
        };

        return this.zs.createZoomer(options);
    }

    getZoomer() {
        return this.zoomer;
    }

    findZoomEventListener(ev) {
        for (let i = 0, len = this.zoomEventListeners.length; i < len; i++) {
            if (this.zoomEventListeners[i] === ev) {
                return i;
            }
        }
        return -1;
    }

    addZoomEventListener(callback) {
        this.zoomEventListeners.push(callback);
    }

    removeZoomEventListener(callback) {
        const evIndex = this.findZoomEventListener(callback);

        if (evIndex !== -1) {
            this.zoomEventListeners.splice(evIndex);
        }
    }

    adjustmentScale(min: number, max: number): number {
        let _scale = 1;
        const size = (min + max) / 2;

        if (size * this.scale() < max) {
            _scale = min / (size * this.scale());
        } else if (size * this.scale() > max) {
            _scale = min / (size * this.scale());
        }

        return _scale;
    }

    scale(): number {
        return this.zoomer.scale();
    }

    panAndZoom(translate: number[], scale: number, transition?: number) {
        this.zoomer.panZoom(translate, scale, transition);
    }

}
