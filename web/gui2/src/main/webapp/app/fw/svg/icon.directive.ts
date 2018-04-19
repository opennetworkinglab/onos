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
import { Directive, ElementRef, Input, OnInit } from '@angular/core';
import { IconService } from './icon.service';
import { LogService } from '../../log.service';
import * as d3 from 'd3';

/**
 * ONOS GUI -- SVG -- Icon Directive
 *
 * TODO: Deprecated - this directive may be removed altogether as it has been
 * rebuilt as IconComponent instead
 */
@Directive({
  selector: '[onosIcon]'
})
export class IconDirective implements OnInit {
    @Input() iconId: string;
    @Input() iconSize: number = 20;

    constructor(
        private el: ElementRef,
        private is: IconService,
        private log: LogService
    ) {
        // Note: iconId is not available until initialization
        this.log.debug('IconDirective constructed');
    }

    ngOnInit() {
        const div = d3.select(this.el.nativeElement);
        div.selectAll('*').remove();
        this.is.loadEmbeddedIcon(div, this.iconId, this.iconSize);
        this.log.debug('IconDirective initialized:', this.iconId);
    }

}
