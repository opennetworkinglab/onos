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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { EeService } from './ee.service';
import { FnService } from './fn.service';
import { KeyService } from './key.service';
import { LionService } from './lion.service';
import { PrefsService } from './prefs.service';
import { RandomService } from './random.service';
import { ThemeService } from './theme.service';

/**
 * ONOS GUI -- Utilities Module
 */
@NgModule({
  imports: [
    CommonModule
  ],
  declarations: [],
  providers: [
    EeService,
    FnService,
    KeyService,
    LionService,
    PrefsService,
    RandomService,
    ThemeService
  ]
})
export class UtilModule { }
