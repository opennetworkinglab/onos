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

import { GeoDataService } from './geodata.service';
import { GlyphService } from './glyph.service';
import { GlyphDataService } from './glyphdata.service';
import { IconService } from './icon.service';
import { MapService } from './map.service';
import { SpriteService } from './sprite.service';
import { SpriteDataService } from './spritedata.service';
import { SvgUtilService } from './svgutil.service';
import { IconComponent } from './icon/icon.component';

/**
 * ONOS GUI -- Scalable Vector Graphics Module
 */
@NgModule({
  exports: [
    IconComponent
  ],
  imports: [
    CommonModule
  ],
  declarations: [
    IconComponent
  ],
  providers: [
    GeoDataService,
    GlyphService,
    GlyphDataService,
    IconService,
    MapService,
    SpriteService,
    SpriteDataService,
    SvgUtilService
  ]
})
export class SvgModule { }
