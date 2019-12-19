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
import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { IconService, glyphMapping } from '../icon.service';
import { LogService } from '../../log.service';

/**
 * Icon Component
 *
 * Note: This is an alternative to the Icon Directive from ONOS 1.0.0
 * It has been implemented as a Component because it was inadvertently adding
 * in a template through d3 DOM manipulations - it's better to make it a Comp
 * and build a template the Angular 7 way
 *
 * Remember: The CSS files applied here only apply to this component
 */
@Component({
  selector: 'onos-icon',
  templateUrl: './icon.component.html',
  styleUrls: [
    './icon.component.css', './icon.theme.css',
    './glyph.css', './glyph-theme.css',
    './tooltip.css', './button-theme.css'
    ]
})
export class IconComponent implements OnChanges {
    @Input() iconId: string;
    @Input() iconSize: number = 20;
    @Input() toolTip: string = undefined;
    @Input() classes: string = undefined;

    // The displayed tooltip - undefined until mouse hovers over, then equals toolTip
    toolTipDisp: string = undefined;

    constructor(
        private is: IconService,
        private log: LogService
    ) {
        // Note: iconId is not available until initialization
    }

    /**
     * This is needed in case the iconId changes while icon component
     * is displayed on screen.
     */
    ngOnChanges() {
        this.is.loadIconDef(this.iconId);
    }
}
