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
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FnService } from '../util/fn.service';
import { LogService } from '../log.service';
import {UrlFnService} from "../remote/urlfn.service";

export interface UiView {
    id: string;
    icon: string;
    cat: string;
    label: string;
}

/**
 * ONOS GUI -- Navigation Service
 */
@Injectable({
  providedIn: 'root',
})
export class NavService {
    public showNav = false;

    uiPlatformViews = new Array<UiView>();
    uiNetworkViews = new Array<UiView>();
    uiOtherViews = new Array<UiView>();
    uiHiddenViews = new Array<UiView>();

    constructor(
        private _fn_: FnService,
        private log: LogService,
        private ufs: UrlFnService,
        private httpClient: HttpClient,
    ) {
        this.log.debug('NavService constructed');
    }

    hideNav() {
        this.showNav = false;
        this.log.debug('Hiding Nav menu');
    }

    toggleNav() {
        this.showNav = !this.showNav;
        if (this.showNav) {
            this.log.debug('Showing Nav menu');
            this.getUiViews();
        } else {
            this.log.debug('Hiding Nav menu');
        }
    }

    getUiViews() {
        this.uiPlatformViews = new Array<UiView>();
        this.uiNetworkViews = new Array<UiView>();
        this.uiOtherViews = new Array<UiView>();
        this.uiHiddenViews = new Array<UiView>();
        this.httpClient.get(this.ufs.rsUrl('nav/uiextensions')).subscribe((v: UiView[]) => {
            v.forEach((uiView: UiView) => {
                if (uiView.cat === 'PLATFORM') {
                    this.uiPlatformViews.push(uiView);
                } else if (uiView.cat === 'NETWORK') {
                    if ( uiView.id !== 'topo') {
                        this.uiNetworkViews.push(uiView);
                    } else {
                        this.uiNetworkViews.push(<UiView>{
                            id: 'topo2',
                            icon: 'nav_topo',
                            cat: 'NETWORK',
                            label: uiView.label
                        });
                    }
                } else if (uiView.cat === 'HIDDEN') {
                    this.uiHiddenViews.push(uiView);
                } else {
                    this.uiOtherViews.push(uiView);
                }
            });
        });
    }

}
