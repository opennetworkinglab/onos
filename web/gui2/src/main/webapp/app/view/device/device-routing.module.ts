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
import { Routes, RouterModule } from '@angular/router';
import { DeviceComponent } from './device/device.component';

const deviceRoutes: Routes = [
    {
        path: '',
        component: DeviceComponent
    }
];

/**
 * ONOS GUI -- Devices Tabular View Feature Routing Module - allows it to be lazy loaded
 *
 * See https://angular.io/guide/lazy-loading-ngmodules
 */
@NgModule({
    imports: [RouterModule.forChild(deviceRoutes)],
    exports: [RouterModule]
})
export class DeviceRoutingModule { }
