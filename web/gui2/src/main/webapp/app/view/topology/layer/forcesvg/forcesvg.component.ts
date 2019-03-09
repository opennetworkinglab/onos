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
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    OnInit,
    Output,
    QueryList,
    SimpleChange,
    SimpleChanges,
    ViewChildren
} from '@angular/core';
import {
    LocMeta,
    LogService,
    MetaUi,
    WebSocketService,
    ZoomUtils
} from 'gui2-fw-lib';
import {
    Device,
    ForceDirectedGraph,
    Host,
    HostLabelToggle,
    LabelToggle,
    LayerType,
    Link,
    LinkHighlight,
    Location,
    ModelEventMemo,
    ModelEventType,
    Region,
    RegionLink,
    SubRegion,
    UiElement
} from './models';
import {
    DeviceNodeSvgComponent,
    HostNodeSvgComponent,
    LinkSvgComponent
} from './visuals';
import {LocationType} from '../backgroundsvg/backgroundsvg.component';

interface UpdateMeta {
    id: string;
    class: string;
    memento: MetaUi;
}

/**
 * ONOS GUI -- Topology Forces Graph Layer View.
 *
 * The regionData is set by Topology Service on WebSocket topo2CurrentRegion callback
 * This drives the whole Force graph
 */
@Component({
    selector: '[onos-forcesvg]',
    templateUrl: './forcesvg.component.html',
    styleUrls: ['./forcesvg.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForceSvgComponent implements OnInit, OnChanges {
    @Input() deviceLabelToggle: LabelToggle = LabelToggle.NONE;
    @Input() hostLabelToggle: HostLabelToggle = HostLabelToggle.NONE;
    @Input() showHosts: boolean = false;
    @Input() highlightPorts: boolean = true;
    @Input() onosInstMastership: string = '';
    @Input() visibleLayer: LayerType = LayerType.LAYER_DEFAULT;
    @Input() selectedLink: RegionLink = null;
    @Input() scale: number = 1;
    @Input() regionData: Region = <Region>{devices: [ [], [], [] ], hosts: [ [], [], [] ], links: []};
    @Output() linkSelected = new EventEmitter<RegionLink>();
    @Output() selectedNodeEvent = new EventEmitter<UiElement>();
    private graph: ForceDirectedGraph;
    private _options: { width, height } = { width: 800, height: 600 };

    // References to the children of this component - these are created in the
    // template view with the *ngFor and we get them by a query here
    @ViewChildren(DeviceNodeSvgComponent) devices: QueryList<DeviceNodeSvgComponent>;
    @ViewChildren(HostNodeSvgComponent) hosts: QueryList<HostNodeSvgComponent>;
    @ViewChildren(LinkSvgComponent) links: QueryList<LinkSvgComponent>;

    constructor(
        protected log: LogService,
        private ref: ChangeDetectorRef,
        protected wss: WebSocketService
    ) {
        this.selectedLink = null;
        this.log.debug('ForceSvgComponent constructed');
    }

    /**
     * Utility for extracting a node name from an endpoint string
     * In some cases - have to remove the port number from the end of a device
     * name
     * @param endPtStr The end point name
     */
    private static extractNodeName(endPtStr: string): string {
        const slash: number = endPtStr.indexOf('/');
        if (slash === -1) {
            return endPtStr;
        } else {
            const afterSlash = endPtStr.substr(slash + 1);
            if (afterSlash === 'None') {
                return endPtStr;
            } else {
                return endPtStr.substr(0, slash);
            }
        }
    }

    /**
     * Recursive method to compare 2 objects attribute by attribute and update
     * the first where a change is detected
     * @param existingNode 1st object
     * @param updatedNode 2nd object
     */
    private static updateObject(existingNode: Object, updatedNode: Object): number {
        let changed: number = 0;
        for (const key of Object.keys(updatedNode)) {
            const o = updatedNode[key];
            if (key === 'id') {
                continue;
            } else if (o && typeof o === 'object' && o.constructor === Object) {
                changed += ForceSvgComponent.updateObject(existingNode[key], updatedNode[key]);
            } else if (existingNode[key] !== updatedNode[key]) {
                changed++;
                existingNode[key] = updatedNode[key];
            }
        }
        return changed;
    }

    @HostListener('window:resize', ['$event'])
    onResize(event) {
        this.graph.initSimulation(this.options);
        this.log.debug('Simulation reinit after resize', event);
    }

    /**
     * After the component is initialized create the Force simulation
     * The list of devices, hosts and links will not have been receieved back
     * from the WebSocket yet as this time - they will be updated later through
     * ngOnChanges()
     */
    ngOnInit() {
        // Receiving an initialized simulated graph from our custom d3 service
        this.graph = new ForceDirectedGraph(this.options, this.log);

        /** Binding change detection check on each tick
         * This along with an onPush change detection strategy should enforce
         * checking only when relevant! This improves scripting computation
         * duration in a couple of tests I've made, consistently. Also, it makes
         * sense to avoid unnecessary checks when we are dealing only with
         * simulations data binding.
         */
        this.graph.ticker.subscribe((simulation) => {
            // this.log.debug("Force simulation has ticked", simulation);
            this.ref.markForCheck();
        });
        this.log.debug('ForceSvgComponent initialized - waiting for nodes and links');

    }

    /**
     * When any one of the inputs get changed by a containing component, this
     * gets called automatically. In addition this is called manually by
     * topology.service when a response is received from the WebSocket from the
     * server
     *
     * The Devices, Hosts and SubRegions are all added to the Node list for the simulation
     * The Links are added to the Link list of the simulation.
     * Before they are added the Links are associated with Nodes based on their endPt
     *
     * @param changes - a list of changed @Input(s)
     */
    ngOnChanges(changes: SimpleChanges) {
        if (changes['regionData']) {
            const devices: Device[] =
                changes['regionData'].currentValue.devices[this.visibleLayerIdx()];
            const hosts: Host[] =
                changes['regionData'].currentValue.hosts[this.visibleLayerIdx()];
            const subRegions: SubRegion[] = changes['regionData'].currentValue.subRegion;
            this.graph.nodes = [];
            if (devices) {
                this.graph.nodes = devices;
            }
            if (hosts) {
                this.graph.nodes = this.graph.nodes.concat(hosts);
            }
            if (subRegions) {
                this.graph.nodes = this.graph.nodes.concat(subRegions);
            }

            // If a node has a fixed location then assign it to fx and fy so
            // that it doesn't get affected by forces
            this.graph.nodes
            .forEach((n) => {
                const loc: Location = <Location>n['location'];
                if (loc && loc.locType === LocationType.GEO) {
                    const position: MetaUi =
                        ZoomUtils.convertGeoToCanvas(
                            <LocMeta>{lng: loc.longOrX, lat: loc.latOrY});
                    n.fx = position.x;
                    n.fy = position.y;
                    this.log.debug('Found node', n.id, 'with', loc.locType);
                }
            });

            // Associate the endpoints of each link with a real node
            this.graph.links = [];
            for (const linkIdx of Object.keys(this.regionData.links)) {
                const epA = ForceSvgComponent.extractNodeName(
                                        this.regionData.links[linkIdx].epA);
                this.regionData.links[linkIdx].source =
                    this.graph.nodes.find((node) =>
                        node.id === epA);
                const epB = ForceSvgComponent.extractNodeName(
                    this.regionData.links[linkIdx].epB);
                this.regionData.links[linkIdx].target =
                    this.graph.nodes.find((node) =>
                        node.id === epB);
                this.regionData.links[linkIdx].index = Number(linkIdx);
            }

            this.graph.links = this.regionData.links;

            this.graph.initSimulation(this.options);
            this.graph.initNodes();
            this.graph.initLinks();
            this.log.debug('ForceSvgComponent input changed',
                this.graph.nodes.length, 'nodes,', this.graph.links.length, 'links');
        }

        this.ref.markForCheck();
    }

    /**
     * Get the index of LayerType so it can drive the visibility of nodes and
     * hosts on layers
     */
    visibleLayerIdx(): number {
        const layerKeys: string[] = Object.keys(LayerType);
        for (const idx in layerKeys) {
            if (LayerType[layerKeys[idx]] === this.visibleLayer) {
                return Number(idx);
            }
        }
        return -1;
    }

    selectLink(link: RegionLink): void {
        this.selectedLink = link;
        this.linkSelected.emit(link);
    }

    get options() {
        return this._options = {
            width: window.innerWidth,
            height: window.innerHeight
        };
    }

    /**
     * Iterate through all hosts and devices to deselect the previously selected
     * node. The emit an event to the parent that lets it know the selection has
     * changed.
     * @param selectedNode the newly selected node
     */
    updateSelected(selectedNode: UiElement): void {
        this.log.debug('Node or link selected', selectedNode ? selectedNode.id : 'none');
        this.devices
            .filter((d) =>
                selectedNode === undefined || d.device.id !== selectedNode.id)
            .forEach((d) => d.deselect());
        this.hosts
            .filter((h) =>
                selectedNode === undefined || h.host.id !== selectedNode.id)
            .forEach((h) => h.deselect());

        this.links
            .filter((l) =>
                selectedNode === undefined || l.link.id !== selectedNode.id)
            .forEach((l) => l.deselect());
        // Push the changes back up to parent (Topology Component)
        this.selectedNodeEvent.emit(selectedNode);
    }

    /**
     * We want to filter links to show only those not related to hosts if the
     * 'showHosts' flag has been switched off. If 'showHosts' is true, then
     * display all links.
     */
    filteredLinks(): Link[] {
        return this.regionData.links.filter((h) =>
            this.showHosts ||
            ((<Host>h.source).nodeType !== 'host' &&
            (<Host>h.target).nodeType !== 'host'));
    }

    /**
     * When changes happen in the model, then model events are sent up through the
     * Web Socket
     * @param type - the type of the change
     * @param memo - a qualifier on the type
     * @param subject - the item that the update is for
     * @param data - the new definition of the item
     */
    handleModelEvent(type: ModelEventType, memo: ModelEventMemo, subject: string, data: UiElement): void {
        switch (type) {
            case ModelEventType.DEVICE_ADDED_OR_UPDATED:
                if (memo === ModelEventMemo.ADDED) {
                    this.regionData.devices[this.visibleLayerIdx()].push(<Device>data);
                    this.graph.nodes.push(<Device>data);
                    this.log.debug('Device added', (<Device>data).id);
                } else if (memo === ModelEventMemo.UPDATED) {
                    const oldDevice: Device =
                        this.regionData.devices[this.visibleLayerIdx()]
                            .find((d) => d.id === subject);
                    const changes = ForceSvgComponent.updateObject(oldDevice, <Device>data);
                    if (changes > 0) {
                        this.log.debug('Device ', oldDevice.id, memo, ' - ', changes, 'changes');
                    }
                } else {
                    this.log.warn('Device ', memo, ' - not yet implemented', data);
                }
                break;
            case ModelEventType.HOST_ADDED_OR_UPDATED:
                if (memo === ModelEventMemo.ADDED) {
                    this.regionData.hosts[this.visibleLayerIdx()].push(<Host>data);
                    this.graph.nodes.push(<Host>data);
                    this.log.debug('Host added', (<Host>data).id);
                } else if (memo === ModelEventMemo.UPDATED) {
                    const oldHost: Host = this.regionData.hosts[this.visibleLayerIdx()]
                        .find((h) => h.id === subject);
                    const changes = ForceSvgComponent.updateObject(oldHost, <Host>data);
                    if (changes > 0) {
                        this.log.debug('Host ', oldHost.id, memo, ' - ', changes, 'changes');
                    }
                } else {
                    this.log.warn('Host change', memo, ' - unexpected');
                }
                break;
            case ModelEventType.DEVICE_REMOVED:
                if (memo === ModelEventMemo.REMOVED || memo === undefined) {
                    const removeIdx: number =
                        this.regionData.devices[this.visibleLayerIdx()]
                            .findIndex((d) => d.id === subject);
                    this.regionData.devices[this.visibleLayerIdx()].splice(removeIdx, 1);
                    this.removeRelatedLinks(subject);
                    this.log.debug('Device ', subject, 'removed. Links', this.regionData.links);
                } else {
                    this.log.warn('Device removed - unexpected memo', memo);
                }
                break;
            case ModelEventType.HOST_REMOVED:
                if (memo === ModelEventMemo.REMOVED || memo === undefined) {
                    const removeIdx: number =
                        this.regionData.hosts[this.visibleLayerIdx()]
                            .findIndex((h) => h.id === subject);
                    this.regionData.hosts[this.visibleLayerIdx()].splice(removeIdx, 1);
                    this.removeRelatedLinks(subject);
                    this.log.warn('Host ', subject, 'removed');
                } else {
                    this.log.warn('Host removed - unexpected memo', memo);
                }
                break;
            case ModelEventType.LINK_ADDED_OR_UPDATED:
                if (memo === ModelEventMemo.ADDED &&
                    this.regionData.links.findIndex((l) => l.id === subject) === -1) {
                    const listLen = this.regionData.links.push(<RegionLink>data);
                    const epA = ForceSvgComponent.extractNodeName(
                        this.regionData.links[listLen - 1].epA);
                    this.regionData.links[listLen - 1].source =
                        this.graph.nodes.find((node) =>
                            node.id === epA);
                    const epB = ForceSvgComponent.extractNodeName(
                        this.regionData.links[listLen - 1].epB);
                    this.regionData.links[listLen - 1].target =
                        this.graph.nodes.find((node) =>
                            node.id === epB);
                    this.log.debug('Link added', subject);
                } else if (memo === ModelEventMemo.UPDATED) {
                    const oldLink = this.regionData.links.find((l) => l.id === subject);
                    const changes = ForceSvgComponent.updateObject(oldLink, <RegionLink>data);
                    this.log.debug('Link ', subject, '. Updated', changes, 'items');
                } else {
                    this.log.warn('Link added or updated - unexpected memo', memo);
                }
                break;
            default:
                this.log.error('Unexpected model event', type, 'for', subject);
        }
        this.ref.markForCheck();
        this.graph.initSimulation(this.options);
    }

    private removeRelatedLinks(subject: string) {
        const len = this.regionData.links.length;
        for (let i = 0; i < len; i++) {
            const linkIdx = this.regionData.links.findIndex((l) =>
                (ForceSvgComponent.extractNodeName(l.epA) === subject ||
                    ForceSvgComponent.extractNodeName(l.epB) === subject));
            if (linkIdx >= 0) {
                this.regionData.links.splice(linkIdx, 1);
                this.log.debug('Link ', linkIdx, 'removed on attempt', i);
            }
        }
    }

    /**
     * When traffic monitoring is turned on (A key) highlights will be sent back
     * from the WebSocket through the Traffic Service
     * @param devices - an array of device highlights
     * @param hosts - an array of host highlights
     * @param links - an array of link highlights
     */
    handleHighlights(devices: Device[], hosts: Host[], links: LinkHighlight[]): void {

        if (devices.length > 0) {
            this.log.debug(devices.length, 'Devices highlighted');
            devices.forEach((dh) => {
                const deviceComponent: DeviceNodeSvgComponent = this.devices.find((d) => d.device.id === dh.id );
                if (deviceComponent) {
                    deviceComponent.ngOnChanges(
                        {'deviceHighlight': new SimpleChange(<Device>{}, dh, true)}
                    );
                    this.log.debug('Highlighting device', deviceComponent.device.id);
                } else {
                    this.log.warn('Device component not found', dh.id);
                }
            });
        }
        if (hosts.length > 0) {
            this.log.debug(hosts.length, 'Hosts highlighted');
            hosts.forEach((hh) => {
                const hostComponent: HostNodeSvgComponent = this.hosts.find((h) => h.host.id === hh.id );
                if (hostComponent) {
                    hostComponent.ngOnChanges(
                        {'hostHighlight': new SimpleChange(<Host>{}, hh, true)}
                    );
                    this.log.debug('Highlighting host', hostComponent.host.id);
                }
            });
        }
        if (links.length > 0) {
            this.log.debug(links.length, 'Links highlighted');
            links.forEach((lh) => {
                const linkComponent: LinkSvgComponent = this.links.find((l) => l.link.id === lh.id );
                if (linkComponent) { // A link might not be present is hosts viewing is switched off
                    linkComponent.ngOnChanges(
                        {'linkHighlight': new SimpleChange(<LinkHighlight>{}, lh, true)}
                    );
                    // this.log.debug('Highlighting link', linkComponent.link.id, lh.css, lh.label);
                }
            });
        }
    }

    /**
     * As nodes are dragged around the graph, their new location should be sent
     * back to server
     * @param klass The class of node e.g. 'host' or 'device'
     * @param id - the ID of the node
     * @param newLocation - the new Location of the node
     */
    nodeMoved(klass: string, id: string, newLocation: MetaUi) {
        this.wss.sendEvent('updateMeta', <UpdateMeta>{
            id: id,
            class: klass,
            memento: newLocation
        });
        this.log.debug(klass, id, 'has been moved to', newLocation);
    }

    resetNodeLocations() {
        this.devices.forEach((d) => {
            d.resetNodeLocation();
        });
    }
}

