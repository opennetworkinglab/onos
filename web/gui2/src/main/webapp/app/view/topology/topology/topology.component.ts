/*
 * Copyright 2019-present Open Networking Foundation
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
import {
    Component,
    OnDestroy,
    OnInit, SimpleChange,
    ViewChild
} from '@angular/core';
import * as d3 from 'd3';
import {
    FnService, IconService,
    KeysService,
    KeysToken, LionService,
    LogService,
    PrefsService,
    SvgUtilService,
    WebSocketService,
    ZoomService
} from 'gui2-fw-lib';
import {InstanceComponent} from '../panel/instance/instance.component';
import {SummaryComponent} from '../panel/summary/summary.component';
import {DetailsComponent} from '../panel/details/details.component';
import {BackgroundSvgComponent} from '../layer/backgroundsvg/backgroundsvg.component';
import {ForceSvgComponent} from '../layer/forcesvg/forcesvg.component';
import {TopologyService} from '../topology.service';
import {
    HostLabelToggle,
    LabelToggle,
    UiElement
} from '../layer/forcesvg/models';
import {ToolbarComponent} from '../panel/toolbar/toolbar.component';
import {TrafficService} from '../traffic.service';
import {ZoomableDirective} from '../layer/zoomable.directive';

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
    @ViewChild(ToolbarComponent) toolbar: ToolbarComponent;
    @ViewChild(BackgroundSvgComponent) background: BackgroundSvgComponent;
    @ViewChild(ForceSvgComponent) force: ForceSvgComponent;
    @ViewChild(ZoomableDirective) zoomDirective: ZoomableDirective;

    flashMsg: string = '';
    prefsState = {};
    hostLabelIdx: number = 1;
    showBackground: boolean = false;
    lionFn; // Function

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected ks: KeysService,
        protected sus: SvgUtilService,
        protected ps: PrefsService,
        protected wss: WebSocketService,
        protected zs: ZoomService,
        protected ts: TopologyService,
        protected trs: TrafficService,
        protected is: IconService,
        private lion: LionService,
    ) {
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-toolbar', () => this.doLion());
        } else {
            this.doLion();
        }

        this.is.loadIconDef('bird');
        this.is.loadIconDef('active');
        this.is.loadIconDef('uiAttached');
        this.is.loadIconDef('m_switch');
        this.is.loadIconDef('m_roadm');
        this.is.loadIconDef('m_router');
        this.is.loadIconDef('m_uiAttached');
        this.is.loadIconDef('m_endstation');
        this.is.loadIconDef('m_ports');
        this.is.loadIconDef('m_summary');
        this.is.loadIconDef('m_details');
        this.is.loadIconDef('m_map');
        this.is.loadIconDef('m_cycleLabels');
        this.is.loadIconDef('m_resetZoom');
        this.is.loadIconDef('m_eqMaster');
        this.is.loadIconDef('m_unknown');
        this.is.loadIconDef('m_allTraffic');
        this.is.loadIconDef('deviceTable');
        this.is.loadIconDef('flowTable');
        this.is.loadIconDef('portTable');
        this.is.loadIconDef('groupTable');
        this.is.loadIconDef('meterTable');
        this.is.loadIconDef('triangleUp');
        this.log.debug('Topology component constructed');
    }

    /**
     * Static functions must come before member variables
     * @param index
     */
    private static deviceLabelFlashMessage(index: number): string {
        switch (index) {
            case 0: return 'fl_device_labels_hide';
            case 1: return 'fl_device_labels_show_friendly';
            case 2: return 'fl_device_labels_show_id';
        }
    }

    private static hostLabelFlashMessage(index: number): string {
        switch (index) {
            case 0: return 'fl_host_labels_hide';
            case 1: return 'fl_host_labels_show_friendly';
            case 2: return 'fl_host_labels_show_ip';
            case 3: return 'fl_host_labels_show_mac';
        }
    }

    /**
     * Pass the list of Key Commands to the KeyService, and initialize the Topology
     * Service - which communicates with through the WebSocket to the ONOS server
     * to get the nodes and links.
     */
    ngOnInit() {
        this.bindCommands();
        // The components from the template are handed over to TopologyService here
        // so that WebSocket responses can be passed back in to them
        // The handling of the WebSocket call is delegated out to the Topology
        // Service just to compartmentalize things a bit
        this.ts.init(this.instance, this.background, this.force);
        this.log.debug('Topology component initialized');
    }

    /**
     * When this component is being stopped, disconnect the TopologyService from
     * the WebSocket
     */
    ngOnDestroy() {
        this.ts.destroy();
        this.log.debug('Topology component destroyed');
    }

    /**
     * When ever a toolbar button is clicked, an event is sent up from toolbar
     * component which is caught and passed on to here.
     * @param name The name of the button that was clicked
     */
    toolbarButtonClicked(name: string) {
        switch (name) {
            case 'instance-tog':
                this.toggleInstancePanel();
                break;
            case 'summary-tog':
                this.toggleSummary();
                break;
            case 'details-tog':
                this.toggleDetails();
                break;
            case 'hosts-tog':
                this.toggleHosts();
                break;
            case 'offline-tog':
                this.toggleOfflineDevices();
                break;
            case 'ports-tog':
                this.togglePorts();
                break;
            case 'bkgrnd-tog':
                this.toggleBackground();
                break;
            case 'cycleLabels-btn':
                this.cycleDeviceLabels();
                break;
            case 'cycleHostLabel-btn':
                this.cycleHostLabels();
                break;
            case 'resetZoom-btn':
                this.resetZoom();
                break;
            case 'eqMaster-btn':
                this.equalizeMasters();
                break;
            case 'cancel-traffic':
                this.cancelTraffic();
                break;
            case 'all-traffic':
                this.monitorAllTraffic();
                break;
            default:
                this.log.warn('Unhandled Toolbar action', name);
        }
    }

    /**
     * The list of key strokes that will be active in the Topology View.
     *
     * This action map is passed to the KeyService through the bindCommands()
     * when this component is being initialized
     */
    actionMap() {
        return {
            A: [() => {this.monitorAllTraffic(); }, 'Monitor all traffic'],
            L: [() => {this.cycleDeviceLabels(); }, 'Cycle device labels'],
            B: [(token) => {this.toggleBackground(token); }, 'Toggle background'],
            D: [(token) => {this.toggleDetails(token); }, 'Toggle details panel'],
            I: [(token) => {this.toggleInstancePanel(token); }, 'Toggle ONOS Instance Panel'],
            O: [() => {this.toggleSummary(); }, 'Toggle the Summary Panel'],
            R: [() => {this.resetZoom(); }, 'Reset pan / zoom'],
            P: [(token) => {this.togglePorts(token); }, 'Toggle Port Highlighting'],
            E: [() => {this.equalizeMasters(); }, 'Equalize mastership roles'],
            X: [() => {this.resetNodeLocation(); }, 'Reset Node Location'],
            U: [() => {this.unpinNode(); }, 'Unpin node (mouse over)'],
            H: [() => {this.toggleHosts(); }, 'Toggle host visibility'],
            M: [() => {this.toggleOfflineDevices(); }, 'Toggle offline visibility'],
            dot: [() => {this.toggleToolbar(); }, 'Toggle Toolbar'],
            0: [() => {this.cancelTraffic(); }, 'Cancel traffic monitoring'],
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

    protected cycleDeviceLabels() {
        const old: LabelToggle = this.force.deviceLabelToggle;
        const next = LabelToggle.next(old);
        this.force.ngOnChanges({'deviceLabelToggle':
                new SimpleChange(old, next, false)});
        this.flashMsg = this.lionFn(TopologyComponent.deviceLabelFlashMessage(next));
        this.log.debug('Cycling device labels', old, next);
    }

    protected cycleHostLabels() {
        const old: HostLabelToggle = this.force.hostLabelToggle;
        const next = HostLabelToggle.next(old);
        this.force.ngOnChanges({'hostLabelToggle':
                new SimpleChange(old, next, false)});
        this.flashMsg = this.lionFn(TopologyComponent.hostLabelFlashMessage(next));
        this.log.debug('Cycling host labels', old, next);
    }

    protected toggleBackground(token?: KeysToken) {
        this.showBackground = !this.showBackground;
        this.flashMsg = this.lionFn(this.showBackground ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_background_map');
        this.toolbar.backgroundVisible = this.showBackground;
        this.log.debug('Toggling background', token);
    }

    protected toggleDetails(token?: KeysToken) {
        if (this.details.selectedNode) {
            const on: boolean = this.details.togglePanel(() => {
            });
            this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
                ' ' + this.lionFn('fl_panel_details');
            this.toolbar.detailsVisible = on;

            this.log.debug('Toggling details', token);
        }
    }

    protected toggleInstancePanel(token?: KeysToken) {
        const on: boolean = this.instance.togglePanel(() => {});
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_panel_instances');
        this.toolbar.instancesVisible = on;
        this.log.debug('Toggling instances', token, on);
    }

    protected toggleSummary() {
        const on: boolean = this.summary.togglePanel(() => {});
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_panel_summary');
        this.toolbar.summaryVisible = on;
    }

    protected resetZoom() {
        this.zoomDirective.resetZoom();
        this.flashMsg = this.lionFn('fl_pan_zoom_reset');
    }

    protected togglePorts(token?: KeysToken) {
        const old: boolean = this.force.highlightPorts;
        const current: boolean = !this.force.highlightPorts;
        this.force.ngOnChanges({'highlightPorts': new SimpleChange(old, current, false)});
        this.flashMsg = this.lionFn(current ? 'enable' : 'disable') +
            ' ' + this.lionFn('fl_port_highlighting');
        this.toolbar.portsVisible = current;
        this.log.debug(current ? 'Enable' : 'Disable', 'port highlighting');
    }

    protected equalizeMasters() {
        this.wss.sendEvent('equalizeMasters', null);
        this.flashMsg = this.lionFn('fl_eq_masters');
        this.log.debug('equalizing masters');
    }

    protected resetNodeLocation() {
        // TODO: Implement reset locations
        this.flashMsg = this.lionFn('fl_reset_node_locations');
        this.log.debug('resetting node location');
    }

    protected unpinNode() {
        // TODO: Implement this
        this.log.debug('unpinning node');
    }

    protected toggleToolbar() {
        this.log.debug('toggling toolbar');
        this.toolbar.on = !this.toolbar.on;
    }

    protected toggleHosts() {
        const old: boolean = this.force.showHosts;
        const current = !this.force.showHosts;
        this.force.ngOnChanges({'showHosts': new SimpleChange(old, current, false)});
        this.flashMsg = this.lionFn('hosts') + ' ' +
                        this.lionFn(this.force.showHosts ? 'visible' : 'hidden');
        this.toolbar.hostsVisible = current;
        this.log.debug('toggling hosts: ', this.force.showHosts ? 'Show' : 'Hide');
    }

    protected toggleOfflineDevices() {
        // TODO: Implement toggle offline visibility
        const on: boolean = true;
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_offline_devices');
        this.log.debug('toggling offline devices');
    }

    /**
     * Check to see if this is needed anymore
     * @param what
     */
    protected notValid(what) {
        this.log.warn('topo.js getActionEntry(): Not a valid ' + what);
    }

    /**
     * Check to see if this is needed anymore
     * @param what
     */
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

    /**
     * An event handler that updates the details panel as items are
     * selected in the forcesvg layer
     * @param nodeOrLink the item to display details of
     */
    nodeSelected(nodeOrLink: UiElement) {
        this.details.ngOnChanges({'selectedNode':
            new SimpleChange(undefined, nodeOrLink, true)});
        this.details.on = Boolean(nodeOrLink);
    }

    /**
     * Enable traffic monitoring
     */
    monitorAllTraffic() {
        // TODO: Implement support for toggling between bits, packets and octets
        this.flashMsg = this.lionFn('tr_fl_pstats_bits');
        this.trs.init(this.force);
    }

    /**
     * Cancel traffic monitoring
     */
    cancelTraffic() {
        this.flashMsg = this.lionFn('fl_monitoring_canceled');
        this.trs.destroy();
    }

    /**
     * Read the LION bundle for Toolbar and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Topo');
    }

    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
