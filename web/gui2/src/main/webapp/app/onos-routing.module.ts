/*
 * Copyright 2018-present Open Networking Foundation
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
import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';

/**
 * The set of Routes in the application - can be chosen from nav menu or
 * elsewhere like tabular icon for flows etc
 */
const onosRoutes: Route[] = [
    <Route>{
        path: 'app',
        pathMatch: 'full',
        loadChildren: () => import('./view/apps/apps.module').then(m => m.AppsModule)
    },
    <Route>{
        path: 'processor',
        pathMatch: 'full',
        loadChildren: () => import('./view/processor/processor.module').then(m => m.ProcessorModule)
    },
    <Route>{
        path: 'settings',
        pathMatch: 'full',
        loadChildren: () => import('./view/settings/settings.module').then(m => m.SettingsModule)
    },
    <Route>{
        path: 'partition',
        pathMatch: 'full',
        loadChildren: () => import('./view/partition/partition.module').then(m => m.PartitionModule)
    },
    <Route>{
        path: 'cluster',
        pathMatch: 'full',
        loadChildren: () => import('./view/cluster/cluster.module').then(m => m.ClusterModule)
    },
    <Route>{
        path: 'device',
        pathMatch: 'full',
        loadChildren: () => import('./view/device/device.module').then(m => m.DeviceModule)
    },
    <Route>{
        path: 'link',
        pathMatch: 'full',
        loadChildren: () => import('./view/link/link.module').then(m => m.LinkModule)
    },
    <Route>{
        path: 'host',
        pathMatch: 'full',
        loadChildren: () => import('./view/host/host.module').then(m => m.HostModule)
    },
    <Route>{
        path: 'intent',
        pathMatch: 'full',
        loadChildren: () => import('./view/intent/intent.module').then(m => m.IntentModule)
    },
    <Route>{
        path: 'tunnel',
        pathMatch: 'full',
        loadChildren: () => import('./view/tunnel/tunnel.module').then(m => m.TunnelModule)
    },
    <Route>{
        path: 'flow',
        pathMatch: 'full',
        loadChildren: () => import('./view/flow/flow.module').then(m => m.FlowModule)
    },
    <Route>{
        path: 'port',
        pathMatch: 'full',
        loadChildren: () => import('./view/port/port.module').then(m => m.PortModule)
    },
    <Route>{
        path: 'group',
        pathMatch: 'full',
        loadChildren: () => import('./view/group/group.module').then(m => m.GroupModule)
    },
    <Route>{
        path: 'meter',
        pathMatch: 'full',
        loadChildren: () => import('./view/meter/meter.module').then(m => m.MeterModule)
    },
    <Route>{
        path: 'pipeconf',
        pathMatch: 'full',
        loadChildren: () => import('./view/pipeconf/pipeconf.module').then(m => m.PipeconfModule)
    },
    <Route>{
        path: 'topo2',
        pathMatch: 'full',
        loadChildren: () => import('../../../../../gui2-topo-lib/lib/gui2-topo-lib.module').then(m => m.Gui2TopoLibModule)
    },
    <Route>{
        path: 'alarmTable',
        pathMatch: 'full',
        loadChildren: () => import('../../../../../../apps/faultmanagement/fm-gui2-lib/lib/fm-gui2-lib.module').then(m => m.FmGui2LibModule)
    },
    <Route>{
        path: 'roadm-gui',
        pathMatch: 'prefix',
        loadChildren: () => import('../../../../../../apps/roadm/web/roadm-gui/lib/roadm-gui-lib.module').then(m => m.RoadmGuiLibModule)
    },
    <Route>{
        path: 'yangModel',
        pathMatch: 'full',
        loadChildren: () => import('../../../../../../apps/yang-gui/yang-gui2-lib/lib/yang-gui2-lib.module').then(m => m.YangGui2LibModule)
    },
    <Route>{
        path: 'intApp',
        pathMatch: 'prefix',
        loadChildren: () => import('../../../../../../apps/inbandtelemetry/intApp-gui2/intApp/lib/intapp-gui2-lib.module').then(m => m.intAppGui2LibModule)
    },
    <Route>{
        path: '',
        redirectTo: 'topo2', // Default to Topology view
        pathMatch: 'full'
    },
];

/**
 * ONOS GUI -- Main Routing Module - allows modules to be lazy loaded
 *
 * See https://angular.io/guide/lazy-loading-ngmodules
 * for the theory of operation
 */
@NgModule({
    imports: [
        RouterModule.forRoot(onosRoutes, {useHash: true})
    ],
    exports: [RouterModule],
    providers: []
})
export class OnosRoutingModule {
}
