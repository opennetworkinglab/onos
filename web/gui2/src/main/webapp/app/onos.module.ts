/*
 * Copyright 2014-present Open Networking Foundation
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
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AppsModule } from './view/apps/apps.module';
import { DeviceModule } from './view/device/device.module';

import { LayerModule } from './fw/layer/layer.module';
import { MastModule } from './fw/mast/mast.module';
import { NavModule } from './fw/nav/nav.module';
import { SvgModule } from './fw/svg/svg.module';
import { RemoteModule } from './fw/remote/remote.module';
import { UtilModule } from './fw/util/util.module';
import { WidgetModule } from './fw/widget/widget.module';

import { AppsComponent } from './view/apps/apps.component';
import { DeviceComponent } from './view/device/device.component';
import { OnosComponent } from './onos.component';
import { DetectBrowserDirective } from './detectbrowser.directive';

import { ConsoleLoggerService } from './consolelogger.service';
import { LogService } from './log.service';
import { OnosService } from './onos.service';

const onosRoutes: Routes = [
  { path: 'apps', component: AppsComponent },        // All except default should be driven by
  { path: 'device', component: DeviceComponent },    // servlet like {INJECTED-VIEW-DATA-START}
  { path: '**', component: DeviceComponent } //Change to Topo(2) when it's ready for normal behaviour
]

/**
 * ONOS GUI -- Main Application Module
 */
@NgModule({
  declarations: [
    OnosComponent,
    DetectBrowserDirective
  ],
  imports: [
    AppsModule,
    DeviceModule,
    BrowserModule,
    BrowserAnimationsModule,
    LayerModule,
    MastModule,
    NavModule,
    RouterModule.forRoot(onosRoutes, { enableTracing: false }),
    SvgModule,
    RemoteModule,
    UtilModule, // For OnosComponent
    WidgetModule
  ],
  providers: [
    OnosService,
    { provide: LogService, useClass: ConsoleLoggerService },
    { provide: Window, useValue: window }
  ],
  bootstrap: [
    OnosComponent,
  ]
})
export class OnosModule { }
