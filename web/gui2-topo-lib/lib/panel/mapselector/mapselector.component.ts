
import {
    Component, EventEmitter,
    OnDestroy,
    OnInit, Output,
} from '@angular/core';
import {
    DetailsPanelBaseImpl,
    FnService,
    LionService,
    LogService,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {FormControl, FormGroup} from '@angular/forms';
import { MapObject } from '../../layer/maputils';

interface MapSelection {
    order: string[];
    maps: Object[];
}

@Component({
    selector: 'onos-mapselector',
    templateUrl: './mapselector.component.html',
    styleUrls: ['./mapselector.component.css', './mapselector.theme.css', '../../topology.common.css']
})
export class MapSelectorComponent extends DetailsPanelBaseImpl implements OnInit, OnDestroy {
    @Output() chosenMap = new EventEmitter<MapObject>();
    lionFn; // Function
    mapSelectorResponse: MapSelection = <MapSelection>{
        order: [],
        maps: []
    };
    form = new FormGroup({
        mapid: new FormControl(this.mapSelectorResponse.order[0]),
    });

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        private lion: LionService
    ) {
        super(fs, log, wss, 'topo');

        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('topoms', () => this.doLion());
        } else {
            this.doLion();
        }

        this.log.debug('Topo MapSelectorComponent constructed');
    }

    ngOnInit() {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['mapSelectorResponse', (data) => {
                this.mapSelectorResponse = data;
                this.form.setValue({'mapid': this.mapSelectorResponse.order[0]});
            }
            ]
        ]));
        this.wss.sendEvent('mapSelectorRequest', {});
        this.log.debug('Topo MapSelectorComponent initialized');
    }

    /**
     * When the component is being unloaded then unbind the WSS handler.
     */
    ngOnDestroy(): void {
        this.wss.unbindHandlers(['mapSelectorResponse']);
        this.log.debug('Topo MapSelectorComponent destroyed');
    }

    /**
     * Read the LION bundle for panel and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Topo');
    }

    choice(mapid: Object): void {
        if (mapid) {
            this.chosenMap.emit(<MapObject>this.mapSelectorResponse.maps[mapid['mapid']]);
        } else {
            this.chosenMap.emit(<MapObject>{});
        }
    }
}
