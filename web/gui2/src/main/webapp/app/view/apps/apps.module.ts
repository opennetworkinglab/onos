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
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AppsComponent } from './apps/apps.component';
import { AppsDetailsComponent } from './appsdetails/appsdetails.component';
import { Gui2FwLibModule } from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * ONOS GUI -- Apps View Module
 *
 * Note: This has been updated from onos-gui-1.0.0 where it was called 'app'
 * whereas here it is now called 'apps'. This is to avoid any confusion with
 * the 'app' folder which is the root of the complete framework
 */
@NgModule({
    imports: [
        CommonModule,
        Gui2FwLibModule,
        RouterModule.forChild([{path: '', component: AppsComponent}]),
        FormsModule
    ],
    declarations: [
        AppsComponent,
        AppsDetailsComponent
    ]
})
export class AppsModule { }
