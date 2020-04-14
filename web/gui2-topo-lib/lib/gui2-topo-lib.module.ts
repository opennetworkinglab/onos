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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TopologyRoutingModule } from './topology-routing.module';
import { TopologyComponent } from './topology/topology.component';
import { NoDeviceConnectedSvgComponent } from './layer/nodeviceconnectedsvg/nodeviceconnectedsvg.component';
import { InstanceComponent } from './panel/instance/instance.component';
import { SummaryComponent } from './panel/summary/summary.component';
import { ToolbarComponent } from './panel/toolbar/toolbar.component';
import { DetailsComponent } from './panel/details/details.component';
import { Gui2FwLibModule } from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { BackgroundSvgComponent } from './layer/backgroundsvg/backgroundsvg.component';
import { ForceSvgComponent } from './layer/forcesvg/forcesvg.component';
import { MapSvgComponent } from './layer/mapsvg/mapsvg.component';
import { TopologyService } from './topology.service';
import { DraggableDirective } from './layer/forcesvg/draggable/draggable.directive';
import { MapSelectorComponent } from './panel/mapselector/mapselector.component';
import { DeviceNodeSvgComponent} from './layer/forcesvg/visuals/devicenodesvg/devicenodesvg.component';
import { HostNodeSvgComponent } from './layer/forcesvg/visuals/hostnodesvg/hostnodesvg.component';
import { SubRegionNodeSvgComponent } from './layer/forcesvg/visuals/subregionnodesvg/subregionnodesvg.component';
import { LinkSvgComponent} from './layer/forcesvg/visuals/linksvg/linksvg.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { GridsvgComponent } from './layer/gridsvg/gridsvg.component';
import {TrafficService} from './traffic.service';
import {LayoutService} from './layout.service';
import { BadgeSvgComponent } from './layer/forcesvg/visuals/badgesvg/badgesvg.component';

/**
 * ONOS GUI -- Topology View Module
 *
 * The main entry point is the TopologyComponent
 *
 * Note: This has been updated from onos-gui-1.0.0 where it was called 'topo2'
 * whereas here it is now called 'topology'. This also merges in the old 'topo'
 */
@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        TopologyRoutingModule,
        Gui2FwLibModule
    ],
    declarations: [
        BackgroundSvgComponent,
        DetailsComponent,
        DeviceNodeSvgComponent,
        ForceSvgComponent,
        GridsvgComponent,
        HostNodeSvgComponent,
        InstanceComponent,
        LinkSvgComponent,
        MapSelectorComponent,
        MapSvgComponent,
        NoDeviceConnectedSvgComponent,
        SubRegionNodeSvgComponent,
        SummaryComponent,
        ToolbarComponent,
        TopologyComponent,
        DraggableDirective,
        BadgeSvgComponent,
    ],
    providers: [
        TopologyService,
        TrafficService,
        LayoutService
    ],
    exports: [
        BackgroundSvgComponent,
        DetailsComponent,
        DeviceNodeSvgComponent,
        ForceSvgComponent,
        GridsvgComponent,
        HostNodeSvgComponent,
        InstanceComponent,
        LinkSvgComponent,
        MapSelectorComponent,
        MapSvgComponent,
        NoDeviceConnectedSvgComponent,
        SubRegionNodeSvgComponent,
        SummaryComponent,
        ToolbarComponent,
        TopologyComponent,
        DraggableDirective,
        BadgeSvgComponent
    ]
})
export class Gui2TopoLibModule { }
