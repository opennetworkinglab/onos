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
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';

import { OnosRoutingModule } from './onos-routing.module';
import { NavComponent } from './nav/nav.component';
import { OnosComponent } from './onos.component';
import {
    Gui2FwLibModule,
    ConsoleLoggerService,
    LogService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { OnosService } from './onos.service';

/**
 * ONOS GUI -- Main Application Module
 */
@NgModule({
    declarations: [
        NavComponent,
        OnosComponent
    ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        Gui2FwLibModule,
        OnosRoutingModule
    ],
    providers: [
        OnosService,
        {provide: LogService, useClass: ConsoleLoggerService},
        {provide: 'Window', useValue: window}
    ],
    bootstrap: [
        OnosComponent,
    ]
})
export class OnosModule {
}
