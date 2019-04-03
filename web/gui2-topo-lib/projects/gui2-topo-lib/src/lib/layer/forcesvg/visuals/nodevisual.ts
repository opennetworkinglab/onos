/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {EventEmitter} from '@angular/core';
import {UiElement} from '../models';

export interface SelectedEvent {
    uiElement: UiElement;
    deselecting: boolean;
    isShift: boolean;
    isCtrl: boolean;
    isAlt: boolean;
}

/**
 * A base class for the Host and Device components
 */
export abstract class NodeVisual {
    selected: boolean;
    selectedEvent = new EventEmitter<SelectedEvent>();

    toggleSelected(uiElement: UiElement, event: MouseEvent) {
        this.selected = !this.selected;
        this.selectedEvent.emit(<SelectedEvent>{
            uiElement: uiElement,
            deselecting: !this.selected,
            isShift: event.shiftKey,
            isCtrl: event.ctrlKey,
            isAlt: event.altKey
        });
    }

    deselect() {
        this.selected = false;
    }
}
