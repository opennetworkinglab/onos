/*
 * Copyright 2019-present Open Networking Foundation
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
import {
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges
} from '@angular/core';

/**
 * How to fit in to the 1000 by 100 SVG viewbox
 */
export enum FitOption {
    FIT1000WIDE = 'fit1000wide',
    FIT1000HIGH = 'fit1000high',
    FITNONE = 'fitnone'// 1:1 ratio
}

const SVG_VIEWBOX_CENTRE = 500; // View box is 0,0,1000,1000

@Component({
    selector: '[onos-gridsvg]',
    templateUrl: './gridsvg.component.html',
    styleUrls: ['./gridsvg.component.css']
})
export class GridsvgComponent implements OnInit, OnChanges {
    @Input() horizLowerLimit: number = 0;
    @Input() horizUpperLimit: number = 1000;
    @Input() vertLowerLimit: number = 0;
    @Input() vertUpperLimit: number = 1000;
    @Input() spacing: number = 100;
    @Input() invertVertical: boolean = false;
    @Input() gridcolor: string = '#e8e7e1'; // If specifying this in a template use [gridcolor]="'#e8e7e1'"
    @Input() centre: boolean = true;
    @Input() fit: FitOption = FitOption.FITNONE;
    @Input() aspectRatio: number = 1.0;

    gridPointsHoriz: number[];
    gridPointsVert: number[];
    horizCentreOffset: number = 0;
    vertCentreOffset: number = 0;
    gridScaleX: number = 1.0;
    gridScaleY: number = 1.0;

    public static calculateGridPoints(lwr: number, upper: number, step: number): number[] {
        const gridPoints = new Array<number>();
        for (let i = lwr; i < upper; i += step) {
            gridPoints.push(i);
        }
        return gridPoints;
    }

    public static calcOffset(lwr: number, upper: number): number {
        return -((upper + lwr) * (upper - lwr) / ((upper - lwr) * 2) - SVG_VIEWBOX_CENTRE);
    }

    public static calcScale(lwr: number, upper: number): number {
        return SVG_VIEWBOX_CENTRE * 2 / Math.abs(upper - lwr);
    }

    constructor() { }

    ngOnInit() {
        this.gridPointsHoriz = GridsvgComponent.calculateGridPoints(
            this.horizLowerLimit, this.horizUpperLimit, this.spacing);
        this.gridPointsVert = GridsvgComponent.calculateGridPoints(
            this.vertLowerLimit, this.vertUpperLimit, this.spacing);
        this.horizCentreOffset = GridsvgComponent.calcOffset(this.horizUpperLimit, this.horizLowerLimit);
        this.vertCentreOffset = GridsvgComponent.calcOffset(this.vertUpperLimit, this.vertLowerLimit);
        this.gridScaleX = this.whichScale(this.fit, true);
        this.gridScaleY = this.whichScale(this.fit, false);
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['horizLowerLimit'] ||
            changes['horizUpperLimit'] ||
            changes['horizSpacing']) {
            this.gridPointsHoriz = GridsvgComponent.calculateGridPoints(
                this.horizLowerLimit, this.horizUpperLimit, this.spacing);
            this.horizCentreOffset = GridsvgComponent.calcOffset(this.horizUpperLimit, this.horizLowerLimit);
        }
        if (changes['vertLowerLimit'] ||
            changes['vertUpperLimit'] ||
            changes['vertSpacing'] ) {
            this.gridPointsVert = GridsvgComponent.calculateGridPoints(
                this.vertLowerLimit, this.vertUpperLimit, this.spacing);
            this.vertCentreOffset = GridsvgComponent.calcOffset(this.vertUpperLimit, this.vertLowerLimit);
        }
    }

    whichScale(fit: FitOption, isX: boolean): number {
        if (fit === FitOption.FIT1000HIGH) {
            return GridsvgComponent.calcScale(
                    this.vertUpperLimit, this.vertLowerLimit) * (isX ? this.aspectRatio : 1.0);
        } else if (fit === FitOption.FIT1000WIDE) {
            return GridsvgComponent.calcScale(
                this.horizUpperLimit, this.horizLowerLimit) * (isX ? 1.0 : this.aspectRatio);
        } else {
            return 1.0;
        }
    }
}
