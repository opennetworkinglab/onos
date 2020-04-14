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
    AfterViewInit,
    Component, HostListener, Inject, Input,
    OnDestroy,
    OnInit, SimpleChange,
    ViewChild
} from '@angular/core';
import * as d3 from 'd3';
import {
    FnService,
    IconService,
    KeysService,
    KeysToken,
    LionService,
    LogService,
    PrefsService,
    SvgUtilService,
    TopoZoomPrefs,
    WebSocketService,
    ZoomUtils
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {InstanceComponent} from '../panel/instance/instance.component';
import {DetailsComponent} from '../panel/details/details.component';
import {BackgroundSvgComponent} from '../layer/backgroundsvg/backgroundsvg.component';
import {ForceSvgComponent} from '../layer/forcesvg/forcesvg.component';
import {Intent, TopologyService} from '../topology.service';
import {
    GridDisplayToggle,
    HostLabelToggle,
    LabelToggle,
    UiElement
} from '../layer/forcesvg/models';
import {
    ALL_TRAFFIC,
    BKGRND_SELECT,
    BKGRND_TOGGLE,
    CANCEL_TRAFFIC,
    CYCLEGRIDDISPLAY_BTN,
    CYCLEHOSTLABEL_BTN,
    CYCLELABELS_BTN,
    DETAILS_TOGGLE,
    EQMASTER_BTN,
    HOSTS_TOGGLE,
    INSTANCE_TOGGLE,
    LAYOUT_ACCESS_BTN,
    LAYOUT_DEFAULT_BTN,
    OFFLINE_TOGGLE,
    PORTS_TOGGLE,
    QUICKHELP_BTN,
    RESETZOOM_BTN,
    SUMMARY_TOGGLE,
    ALARMS_TOGGLE
} from '../panel/toolbar/toolbar.component';
import {TrafficService, TrafficType} from '../traffic.service';
import {ZoomableDirective} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {MapObject} from '../layer/maputils';
import {LayoutService, LayoutType} from '../layout.service';
import {SelectedEvent} from '../layer/forcesvg/visuals/nodevisual';
import {ActivatedRoute} from '@angular/router';

const TOPO2_PREFS = 'topo2_prefs';
const TOPO_MAPID_PREFS = 'topo_mapid';

const PREF_BG = 'bg';
const PREF_DETAIL = 'detail';
const PREF_DLBLS = 'dlbls';
const PREF_HLBLS = 'hlbls';
const PREF_GRID = 'grid';
const PREF_HOSTS = 'hosts';
const PREF_INSTS = 'insts';
const PREF_OFFDEV = 'offdev';
const PREF_PORTHL = 'porthl';
const PREF_SUMMARY = 'summary';
const PREF_TOOLBAR = 'toolbar';
const PREF_PINNED = 'pinned';
const PREF_TRAFFIC = 'traffic';
const PREF_ALARMS = 'alarms';

const BACKGROUND_ELEMENTS = [
    'svg topo2',
    'path bgmap'
];

/**
 * Model of the topo2_prefs object - this is a subset of the overall Prefs returned
 * by the server
 */
export interface Topo2Prefs {
    bg: number;
    detail: number;
    dlbls: number;
    hlbls: number;
    hosts: number;
    insts: number;
    offdev: number;
    porthl: number;
    spr: number;
    ovid: string;
    summary: number;
    toolbar: number;
    grid: number;
    pinned: number;
    traffic: number;
    alarms: number;
}

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
export class TopologyComponent implements OnInit, OnDestroy, AfterViewInit {
    @Input() bannerHeight: number = 48;
    // These are references to the components inserted in the template
    @ViewChild(InstanceComponent, {static: true}) instance: InstanceComponent;
    @ViewChild(DetailsComponent, {static: true}) details: DetailsComponent;
    @ViewChild(BackgroundSvgComponent, {static: true}) background: BackgroundSvgComponent;
    @ViewChild(ForceSvgComponent, {static: true}) force: ForceSvgComponent;
    @ViewChild(ZoomableDirective, {static: true}) zoomDirective: ZoomableDirective;

    flashMsg: string = '';
    // These are used as defaults if nothing is set on the server
    prefsState: Topo2Prefs = <Topo2Prefs>{
        bg: 0,
        detail: 1,
        dlbls: 0,
        hlbls: 2,
        hosts: 0,
        insts: 1,
        offdev: 1,
        ovid: 'traffic', // default to traffic overlay
        porthl: 1,
        spr: 0,
        summary: 1,
        toolbar: 0,
        grid: 0,
        pinned: 0,
        traffic: 2, // default to PORTSTATSPKTSEC, as it will iterate over to 0 on init
        alarms: 1,
    };

    mapIdState: MapObject = <MapObject>{
        id: undefined,
        scale: 1.0
    };
    mapSelShown: boolean = false;
    lionFn; // Function

    gridShown: boolean = true;
    geoGridShown: boolean = true;

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected ks: KeysService,
        protected sus: SvgUtilService,
        protected ps: PrefsService,
        protected wss: WebSocketService,
        protected ts: TopologyService,
        protected trs: TrafficService,
        protected is: IconService,
        private lion: LionService,
        private layout: LayoutService,
        protected ar: ActivatedRoute,
        @Inject('Window') public window: any,
    ) {
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topo-toolbar', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.warn('Constructor', this.zoomDirective);

        this.is.loadIconDef('active');
        this.is.loadIconDef('bgpSpeaker');
        this.is.loadIconDef('bird');
        this.is.loadIconDef('deviceTable');
        this.is.loadIconDef('fiber_switch');
        this.is.loadIconDef('flowTable');
        this.is.loadIconDef('groupTable');
        this.is.loadIconDef('m_allTraffic');
        this.is.loadIconDef('m_cycleLabels');
        this.is.loadIconDef('m_cycleGridDisplay');
        this.is.loadIconDef('m_disjointPaths');
        this.is.loadIconDef('m_details');
        this.is.loadIconDef('m_endstation');
        this.is.loadIconDef('m_eqMaster');
        this.is.loadIconDef('m_fiberSwitch');
        this.is.loadIconDef('m_firewall');
        this.is.loadIconDef('m_map');
        this.is.loadIconDef('m_microwave');
        this.is.loadIconDef('m_ols');
        this.is.loadIconDef('m_otn');
        this.is.loadIconDef('m_ports');
        this.is.loadIconDef('m_resetZoom');
        this.is.loadIconDef('m_roadm');
        this.is.loadIconDef('m_roadm_otn');
        this.is.loadIconDef('m_router');
        this.is.loadIconDef('m_selectMap');
        this.is.loadIconDef('m_summary');
        this.is.loadIconDef('m_switch');
        this.is.loadIconDef('m_terminal_device');
        this.is.loadIconDef('m_uiAttached');
        this.is.loadIconDef('m_unknown');
        this.is.loadIconDef('meterTable');
        this.is.loadIconDef('microwave');
        this.is.loadIconDef('otn');
        this.is.loadIconDef('portTable');
        this.is.loadIconDef('roadm_otn');
        this.is.loadIconDef('triangleUp');
        this.is.loadIconDef('uiAttached');
    }

    /**
     * Static functions must come before member variables
     * @param index Corresponds to LabelToggle.Enum index
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

    private static gridDisplayFlashMessage(index: number): string {
        switch (index) {
            case 0: return 'fl_grid_display_hide';
            case 1: return 'fl_grid_display_1000';
            case 2: return 'fl_grid_display_geo';
            case 3: return 'fl_grid_display_both';
        }
    }

    private static trafficTypeFlashMessage(index: number): string {
        switch (index) {
            case 0: return 'tr_fl_fstats_bytes';
            case 1: return 'tr_fl_pstats_bits';
            case 2: return 'tr_fl_pstats_pkts';
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

        // For the 2.1 release to not listen to updates of prefs as they are
        // only the echo of what we have sent down and the event mechanism
        // does not discern between users. Can get confused if multiple windows open
        // this.ps.addListener((data) => this.prefsUpdateHandler(data));

        this.prefsState = this.ps.getPrefs(TOPO2_PREFS, this.prefsState);
        this.mapIdState = this.ps.getPrefs(TOPO_MAPID_PREFS, this.mapIdState);
        this.trs.init(this.force);

        // Scale the window initially - then after resize
        const zoomMapExtents = ZoomUtils.zoomToWindowSize(
            this.bannerHeight, this.window.innerWidth, this.window.innerHeight);
        this.zoomDirective.changeZoomLevel(zoomMapExtents, true);

        // TODO find out why the following is never printed
        this.log.debug('TopologyComponent initialized,',
            this.bannerHeight, this.window.innerWidth, this.window.innerHeight,
            zoomMapExtents);
    }

    ngAfterViewInit(): void {
        this.ar.queryParams.subscribe(params => {
            const intentId = params['intentId'];
            const intentType = params['intentType'];
            const appId = params['appId'];
            const appName = params['appName'];

            if (intentId && intentType && appId) {
                const selectedIntent = <Intent>{
                    key: intentId,
                    type: intentType,
                    appId: appId,
                    appName: appName,
                };
                this.ts.setSelectedIntent(selectedIntent);

                this.log.warn('TopologyComponent init with Intent: ', selectedIntent, params);
            }
        });
    }

    /**
     * Callback function that's called whenever new Prefs are received from WebSocket
     *
     * Note: At present the backend server does not filter updated by logged in user,
     * so you might get updates pertaining to a different user
     */
    prefsUpdateHandler(data: any): void {
        // Extract the TOPO2 prefs from it
        if (data[TOPO2_PREFS]) {
            this.prefsState = data[TOPO2_PREFS];
        }
        this.log.debug('Updated topo2 prefs', this.prefsState, this.mapIdState);
    }

    /**
     * When this component is being stopped, disconnect the TopologyService from
     * the WebSocket
     */
    ngOnDestroy() {
        this.ts.destroy();
        this.ps.removeListener((data) => this.prefsUpdateHandler(data));
        this.trs.destroy();
        this.log.debug('Topology component destroyed');
    }

    @HostListener('window:resize', ['$event'])
    onResize(event) {
        const zoomMapExtents = ZoomUtils.zoomToWindowSize(
                this.bannerHeight, event.target.innerWidth, event.target.innerHeight);
        this.zoomDirective.changeZoomLevel(zoomMapExtents, true);
        this.log.debug('Topology window resize',
            event.target.innerWidth, event.target.innerHeight, this.bannerHeight, zoomMapExtents);
    }

    /**
     * When ever a toolbar button is clicked, an event is sent up from toolbar
     * component which is caught and passed on to here.
     * @param name The name of the button that was clicked
     */
    toolbarButtonClicked(name: string) {
        switch (name) {
            case INSTANCE_TOGGLE:
                this.toggleInstancePanel();
                break;
            case SUMMARY_TOGGLE:
                this.toggleSummary();
                break;
            case DETAILS_TOGGLE:
                this.toggleDetails();
                break;
            case HOSTS_TOGGLE:
                this.toggleHosts();
                break;
            case OFFLINE_TOGGLE:
                this.toggleOfflineDevices();
                break;
            case PORTS_TOGGLE:
                this.togglePorts();
                break;
            case BKGRND_TOGGLE:
                this.toggleBackground();
                break;
            case BKGRND_SELECT:
                this.mapSelShown = !this.mapSelShown;
                break;
            case CYCLELABELS_BTN:
                this.cycleDeviceLabels();
                break;
            case CYCLEHOSTLABEL_BTN:
                this.cycleHostLabels();
                break;
            case CYCLEGRIDDISPLAY_BTN:
                this.cycleGridDisplay();
                break;
            case RESETZOOM_BTN:
                this.resetZoom();
                break;
            case EQMASTER_BTN:
                this.equalizeMasters();
                break;
            case CANCEL_TRAFFIC:
                this.cancelTraffic();
                break;
            case ALL_TRAFFIC:
                this.cycleTrafficTypeDisplay();
                break;
            case QUICKHELP_BTN:
                this.ks.quickHelpShown = true;
                break;
            case LAYOUT_DEFAULT_BTN:
                this.layout.changeLayout(LayoutType.LAYOUT_DEFAULT);
                break;
            case LAYOUT_ACCESS_BTN:
                this.layout.changeLayout(LayoutType.LAYOUT_ACCESS);
                break;
            case ALARMS_TOGGLE:
                this.toggleAlarms();
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
     *
     * TODO - Replace this doggy doo doo (copied over from GUI-1)
     * with something more structured
     */
    actionMap() {
        return {
            A: [() => {this.cycleTrafficTypeDisplay(); }, this.lionFn('tr_btn_monitor_all')],
            B: [(token) => {this.toggleBackground(token); }, this.lionFn('tbtt_tog_map')],
            D: [(token) => {this.toggleDetails(token); }, this.lionFn('tbtt_tog_use_detail')],
            E: [() => {this.equalizeMasters(); }, this.lionFn('tbtt_eq_master')],
            H: [() => {this.toggleHosts(); }, this.lionFn('tbtt_tog_host')],
            I: [(token) => {this.toggleInstancePanel(token); }, this.lionFn('tbtt_tog_instances')],
            G: [() => {this.mapSelShown = !this.mapSelShown; }, this.lionFn('tbtt_sel_map')],
            L: [() => {this.cycleDeviceLabels(); }, this.lionFn('tbtt_cyc_dev_labs')],
            M: [() => {this.toggleOfflineDevices(); }, this.lionFn('tbtt_tog_offline')],
            O: [() => {this.toggleSummary(); }, this.lionFn('tbtt_tog_summary')],
            P: [(token) => {this.togglePorts(token); }, this.lionFn('tbtt_tog_porthi')],
            Q: [() => {this.cycleGridDisplay(); }, this.lionFn('tbtt_cyc_grid_display')],
            R: [() => {this.resetZoom(); }, this.lionFn('tbtt_reset_zoom')],
            U: [() => {this.unpinOrFreezeNodes(); }, this.lionFn('tbtt_unpin_node')],
            X: [() => {this.resetNodeLocation(); }, this.lionFn('tbtt_reset_loc')],
            dot: [() => {this.toggleToolbar(); }, this.lionFn('tbtt_tog_toolbar')],
            0: [() => {this.cancelTraffic(); }, this.lionFn('tr_btn_cancel_monitoring')],
            'shift-L': [() => {this.cycleHostLabels(); }, this.lionFn('tbtt_cyc_host_labs')],

            // -- instance color palette debug
            9: () => {
                this.sus.cat7().testCard(d3.select('svg#topo2'));
            },

            esc: [() => {this.handleEscape(); }, this.lionFn('qh_hint_esc')],

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
            this.nodeSelected(undefined);
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

    /**
     * Updates the cache of preferences locally and onwards to the PrefsService
     * @param what The attribute of the local topo2-prefs cache to update
     * @param b the value to update it with
     */
    updatePrefsState(what: string, b: number) {
        this.prefsState[what] = b;
        this.ps.setPrefs(TOPO2_PREFS, this.prefsState);
    }

    /**
     * When the button is clicked on the toolbar or the L key is pressed
     * 1) cycle through options
     * 2) flash up a message
     * 3a) Update the local prefs cache
     * 3b) And passes on to the global prefs service which sends back to the server
     * 3c) It also has a knock on effect of passing it on to ForceSvgComponent
     *      because prefsState.dlbls is given as an input to it
     * 3d) This will in turn pass it down to the DeviceSvgComponent which
     *       displays the label
     */
    protected cycleDeviceLabels() {
        const old: LabelToggle.Enum = this.prefsState.dlbls;
        const next = LabelToggle.next(old);
        this.flashMsg = this.lionFn(TopologyComponent.deviceLabelFlashMessage(next));
        this.updatePrefsState(PREF_DLBLS, next);
        this.log.debug('Cycling device labels', old, next);
    }

    protected cycleHostLabels() {
        const old: HostLabelToggle.Enum = this.prefsState.hlbls;
        const next = HostLabelToggle.next(old);
        this.flashMsg = this.lionFn(TopologyComponent.hostLabelFlashMessage(next));
        this.updatePrefsState(PREF_HLBLS, next);
        this.log.debug('Cycling host labels', old, next);
    }

    protected cycleGridDisplay() {
        const old: GridDisplayToggle.Enum = this.prefsState.grid;
        const next = GridDisplayToggle.next(old);
        this.flashMsg = this.lionFn(TopologyComponent.gridDisplayFlashMessage(next));
        this.updatePrefsState(PREF_GRID, next);
        this.log.debug('Cycling grid display', old, next);
    }

    protected cycleTrafficTypeDisplay() {
        const old: TrafficType.Enum = this.prefsState.traffic; // by number
        const next = TrafficType.next(old);
        this.flashMsg = this.lionFn(TopologyComponent.trafficTypeFlashMessage(next));
        this.updatePrefsState(PREF_TRAFFIC, next);
        this.trs.requestTraffic(next);
        this.log.debug('Cycling traffic display', old, next);
    }

    /**
     * When the button is clicked on the toolbar or the B key is pressed
     * 1) Find the inverse of the current state (held as 1 or 0)
     * 2) Flash up a message on screen
     * 3b) And passes on to the global prefs service which sends back to the server
     * 3c) It also has a knock on effect of passing it on to ToolbarComponent
     *      because prefsState.bg is given as an input to it
     * @param token not currently used
     */
    protected toggleBackground(token?: KeysToken) {
        const bg: boolean = !Boolean(this.prefsState.bg);
        this.flashMsg = this.lionFn(bg ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_background_map');
        this.updatePrefsState(PREF_BG, bg ? 1 : 0);
        this.log.debug('Toggling background', token, bg ? 'shown' : 'hidden');
    }

    protected toggleDetails(token?: KeysToken) {
        const on: boolean = !Boolean(this.prefsState.detail);
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_panel_details');
        this.updatePrefsState(PREF_DETAIL, on ? 1 : 0);
        this.log.debug('Toggling details', token);
    }

    protected toggleInstancePanel(token?: KeysToken) {
        const on: boolean = !Boolean(this.prefsState.insts);
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_panel_instances');
        this.updatePrefsState(PREF_INSTS, on ? 1 : 0);
        this.log.debug('Toggling instances', token, on);
    }

    protected toggleSummary() {
        const on: boolean = !Boolean(this.prefsState.summary);
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_panel_summary');
        this.updatePrefsState(PREF_SUMMARY, on ? 1 : 0);
    }

    protected togglePorts(token?: KeysToken) {
        const current: boolean = !Boolean(this.prefsState.porthl);
        this.flashMsg = this.lionFn(current ? 'enable' : 'disable') +
            ' ' + this.lionFn('fl_port_highlighting');
        this.updatePrefsState(PREF_PORTHL, current ? 1 : 0);
        this.log.debug(current ? 'Enable' : 'Disable', 'port highlighting');
    }

    protected toggleToolbar() {
        const on: boolean = !Boolean(this.prefsState.toolbar);
        this.updatePrefsState(PREF_TOOLBAR, on ? 1 : 0);
        this.log.debug('toggling toolbar', on ? 'shown' : 'hidden');
    }

    protected toggleHosts() {
        const current: boolean = !Boolean(this.prefsState.hosts);
        this.flashMsg = this.lionFn('hosts') + ' ' +
                        this.lionFn(current ? 'visible' : 'hidden');
        this.updatePrefsState(PREF_HOSTS, current ? 1 : 0);
        this.log.debug('toggling hosts: ', this.prefsState.hosts ? 'Show' : 'Hide');
    }

    protected toggleOfflineDevices() {
        const on: boolean = !Boolean(this.prefsState.offdev);
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') +
            ' ' + this.lionFn('fl_offline_devices');
        this.updatePrefsState(PREF_OFFDEV, on ? 1 : 0);
        this.log.debug('toggling offline devices', this.prefsState.offdev);
    }


    protected toggleAlarms() {
        const on: boolean = !Boolean(this.prefsState.alarms);
        this.flashMsg = this.lionFn(on ? 'show' : 'hide') + ' Alarms';
        this.updatePrefsState(PREF_ALARMS, on ? 1 : 0);
        this.log.debug('Alarms toggled', on);
    }

    protected resetZoom() {
        const zoomMapExtents = ZoomUtils.zoomToWindowSize(
            this.bannerHeight, this.window.innerWidth, this.window.innerHeight);
        this.zoomDirective.changeZoomLevel(zoomMapExtents, false);
        this.flashMsg = this.lionFn('fl_pan_zoom_reset');
    }

    protected equalizeMasters() {
        this.wss.sendEvent('equalizeMasters', {});
        this.flashMsg = this.lionFn('fl_eq_masters');
        this.log.debug('equalizing masters');
    }

    /**
     * If any nodes with fixed positions had been dragged out of place
     * then put back where they belong
     * If there are some devices selected reset only these
     */
    protected resetNodeLocation() {
        const numNodes = this.force.resetNodeLocations();
        this.flashMsg = this.lionFn('fl_reset_node_locations') +
            '(' + String(numNodes) + ')';
        this.log.debug('resetting ', numNodes, 'node(s) location');
    }

    /**
     * Toggle floating nodes between pinned and frozen
     * If there are floating nodes selected toggle only these
     */
    protected unpinOrFreezeNodes() {
        const pinned: boolean = !Boolean(this.prefsState.pinned);
        const numNodes = this.force.unpinOrFreezeNodes(pinned);
        this.flashMsg = this.lionFn(pinned ?
            'fl_pinned_floating_nodes' : 'fl_unpinned_floating_nodes') +
            '(' + String(numNodes) + ')';
        this.updatePrefsState(PREF_PINNED, pinned ? 1 : 0);
        this.log.debug('Toggling pinning for floating ', numNodes, 'nodes', pinned);
    }

    /**
     * Check to see if this is needed anymore
     * @param what - a key stroke
     */
    protected notValid(what) {
        this.log.warn('topo.js getActionEntry(): Not a valid ' + what);
    }

    /**
     * Check to see if this is needed anymore
     * @param key - a key stroke
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
     *
     * @param nodesOrLink the item(s) to display details of
     */
    nodeSelected(nodesOrLink: UiElement[]) {
        this.details.ngOnChanges({'selectedNodes':
            new SimpleChange(undefined, nodesOrLink, true)});
    }

    /**
     * Cancel traffic monitoring
     */
    cancelTraffic() {
        this.flashMsg = this.lionFn('fl_monitoring_canceled');
        this.trs.cancelTraffic();
    }

    changeMap(map: MapObject) {
        this.mapSelShown = false; // Hide the MapSelector component
        this.mapIdState = map;
        this.ps.setPrefs(TOPO_MAPID_PREFS, this.mapIdState);
        this.log.debug('Map has been changed to ', map);
    }

    mapExtentsZoom(zoomMapExtents: TopoZoomPrefs) {
        // this.zoomDirective.updateZoomState(zoomPrefs.tx, zoomPrefs.ty, zoomPrefs.sc);
        this.zoomDirective.changeZoomLevel(zoomMapExtents);
        this.log.debug('Map zoom prefs updated', zoomMapExtents);
    }

    backgroundClicked(event: MouseEvent) {
        const elemTagName = event.target['tagName'] + ' ' + event.target['id'];
        if (BACKGROUND_ELEMENTS.includes(elemTagName)) {
            this.force.updateSelected(
                <SelectedEvent>{
                    uiElement: undefined,
                    deselecting: true
                }
            );
        }
        this.ts.cancelHighlights();
        this.force.cancelAllLinkHighlightsNow();
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
