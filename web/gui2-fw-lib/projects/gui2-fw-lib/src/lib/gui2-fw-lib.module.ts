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
import {CommonModule} from '@angular/common';
import {DetectBrowserDirective} from './detectbrowser.directive';
import {IconComponent} from './svg/icon/icon.component';
import {VeilComponent} from './layer/veil/veil.component';
import {FlashComponent} from './layer/flash/flash.component';
import {ConfirmComponent} from './layer/confirm/confirm.component';
import {MastComponent} from './mast/mast/mast.component';
import {TableFilterPipe} from './widget/tablefilter.pipe';
import {TableResizeDirective} from './widget/tableresize.directive';
import {QuickhelpComponent} from './layer/quickhelp/quickhelp.component';
import {LoadingComponent} from './layer/loading/loading.component';
import {ZoomableDirective} from './svg/zoomable.directive';
import {NameInputComponent} from './util/name-input/name-input.component';

@NgModule({
    imports: [
        CommonModule
    ],
    declarations: [
        DetectBrowserDirective,
        TableResizeDirective,
        IconComponent,
        VeilComponent,
        FlashComponent,
        ConfirmComponent,
        QuickhelpComponent,
        MastComponent,
        TableFilterPipe,
        LoadingComponent,
        ZoomableDirective,
        NameInputComponent
    ],
    exports: [
        DetectBrowserDirective,
        TableResizeDirective,
        IconComponent,
        VeilComponent,
        FlashComponent,
        ConfirmComponent,
        QuickhelpComponent,
        MastComponent,
        TableFilterPipe,
        LoadingComponent,
        ZoomableDirective,
        NameInputComponent
    ]
})
export class Gui2FwLibModule {
}
