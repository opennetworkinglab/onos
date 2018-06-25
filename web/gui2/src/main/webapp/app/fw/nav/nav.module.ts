/*
 * Copyright 2015-present Open Networking Foundation
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
import { RouterModule, Routes } from '@angular/router';
import { SvgModule } from '../svg/svg.module';
import { OnosRoutingModule } from '../../onos-routing.module';

import { NavComponent } from './nav/nav.component';
import { NavService } from './nav.service';

/**
 * ONOS GUI -- Navigation Module
 */
@NgModule({
  imports: [
    CommonModule,
    OnosRoutingModule,
    RouterModule,
    SvgModule
  ],
  declarations: [
    NavComponent
  ],
  exports: [
    NavComponent
  ],
  providers: [
    NavService
  ]
})
export class NavModule { }
