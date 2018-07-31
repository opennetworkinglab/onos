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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SettingsRoutingModule } from './settings-routing.module';
import { SettingsComponent } from './settings/settings.component';
import { SvgModule } from '../../fw/svg/svg.module';
import { WidgetModule } from '../../fw/widget/widget.module';
import { SettingsDetailsComponent } from './settingsdetails/settingsdetails.component';

@NgModule({
    imports: [
        CommonModule,
        SettingsRoutingModule,
        SvgModule,
        WidgetModule
    ],
    declarations: [SettingsComponent, SettingsDetailsComponent]
})
export class SettingsModule { }
