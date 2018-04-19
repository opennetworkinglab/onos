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
import { UtilModule } from '../util/util.module';

import { DetailsPanelService } from './detailspanel.service';
import { DialogService } from './dialog.service';
import { EditableTextService } from './editabletext.service';
import { FlashService } from './flash.service';
import { LoadingService } from './loading.service';
import { PanelService } from './panel.service';
import { QuickHelpService } from './quickhelp.service';
import { VeilService } from './veil.service';

/**
 * ONOS GUI -- Layers Module
 */
@NgModule({
  imports: [
    CommonModule,
    UtilModule
  ],
  providers: [
    DetailsPanelService,
    DialogService,
    EditableTextService,
    FlashService,
    LoadingService,
    PanelService,
    QuickHelpService,
    VeilService
  ]
})
export class LayerModule { }
