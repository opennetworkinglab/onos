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
import { LayerModule } from '../layer/layer.module';
import { SvgModule } from '../svg/svg.module';

import { ButtonService } from './button.service';
import { ChartBuilderService } from './chartbuilder.service';
import { ListService } from './list.service';
import { ToolbarService } from './toolbar.service';
import { SortableHeaderDirective } from './sortableheader.directive';
import { TableResizeDirective } from './tableresize.directive';
import { FlashChangesDirective } from './flashchanges.directive';
import { TableFilterPipe } from './tablefilter.pipe';

/**
 * ONOS GUI -- Widgets Module
 */
@NgModule({
  imports: [
    CommonModule
    // Services have global scope and their modules don't have to be imported again
    // It's enough to import them in the OnosModule
  ],
  declarations: [
    SortableHeaderDirective,
    TableResizeDirective,
    FlashChangesDirective,
    TableFilterPipe
  ],
  exports: [
    TableFilterPipe
  ],
  providers: [
  ]
})
export class WidgetModule { }
