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
import { Component, OnInit } from '@angular/core';
import { LogService } from '../../../log.service';
import { NavService } from '../nav.service';
import { trigger, state, style, animate, transition } from '@angular/animations';

/**
 * ONOS GUI -- Navigation Module
 */
@Component({
  selector: 'onos-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.theme.css', './nav.component.css'],
  animations: [
    trigger('navState', [
      state('inactive', style({
        visibility: 'hidden',
        transform: 'translateX(-100%)'
      })),
      state('active', style({
        visibility: 'visible',
        transform: 'translateX(0%)'
      })),
      transition('inactive => active', animate('100ms ease-in')),
      transition('active => inactive', animate('100ms ease-out'))
    ])
  ]
})
export class NavComponent implements OnInit {

  constructor(
    private log: LogService,
    public ns: NavService
  ) {
    this.log.debug('NavComponent constructed');
  }

  ngOnInit() {
    this.log.debug('NavComponent initialized');
  }

}
