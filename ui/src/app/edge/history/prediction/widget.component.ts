import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ThrowStmt } from '@angular/compiler';

@Component({
    selector: PredictionComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class PredictionComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "predictionWidget";

    public edge: Edge = null;
    public data: Cumulated = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    };

    protected updateValues() {
        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    this.data = response.result.data;
                }).catch(() => {
                    this.data = null;
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('predictorSolcast0', 'Predict'),
                new ChannelAddress('predictorSolcast0', 'Predict10'),
                new ChannelAddress('predictorSolcast0', 'Predict90'),
                new ChannelAddress('weather0', 'Temperature'),
                new ChannelAddress('weather0', 'Clouds'),
                new ChannelAddress('weewx0', 'Temperature')
            ];
            resolve(channels);
        });
    }
}