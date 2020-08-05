import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'productionTotalDcChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionTotalDcChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super(service, translate);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
        this.subscribeChartRefresh()
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            let indexLastLabel = labels.findIndex(this.getCurrentDate);
            let LabelNow: Date = labels[indexLastLabel - 1];

            // convert datasets
            let datasets = [];

            Object.keys(result.data).forEach((channel) => {
                let channelAddress = ChannelAddress.fromString(channel);
                let data = result.data[channel].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                if (channelAddress.channelId == 'ProductionDcActualPower') {
                    datasets.push({
                        label: this.translate.instant('General.production'),
                        data: data
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(45,143,171,0.05)',
                        borderColor: 'rgba(45,143,171,1)'
                    });
                }
                if (channelAddress.channelId == 'Predict00h') {
                    datasets.push({
                        label: 'Prediction',
                        data: data
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(0,100,0,0.05)',
                        borderColor: 'rgba(0,100,0,1)'
                    });
                }
                if (channelAddress.channelId == 'Predict1000h') {
                    datasets.push({
                        label: 'Prediction 10',
                        data: data
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(125,255,0,0.05)',
                        borderColor: 'rgba(125,255,0,1)'
                    });
                }
                if (channelAddress.channelId == 'Predict9000h') {
                    datasets.push({
                        label: 'Prediction 90',
                        data: data
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(50,200,50,0.05)',
                        borderColor: 'rgba(50,200,50,1)'
                    });
                }
                if (LabelNow && channelAddress.channelId.length == 10 && channelAddress.channelId != 'Predict00h' && channelAddress.channelId.startsWith('Predict') && channelAddress.channelId.endsWith('h')) {
                    let dataset01: Dataset = datasets.find(label => label.label === 'Prediction');
                    let data01: number[] = dataset01.data;
                    let TimeDiff = parseInt(channelAddress.channelId.substring(7, 9), 10);
                    let dt: Date = new Date;
                    dt.setTime(LabelNow.getTime() + TimeDiff * (60 * 60 * 1000));
                    let dtEnd: Date = new Date(this.period.to.getFullYear(), this.period.to.getMonth(), this.period.to.getDate());
                    dtEnd.setDate(dtEnd.getDate() + 1);
                    if (dt < dtEnd) {
                        data01.push(data[indexLastLabel - 1]);
                        if (labels.lastIndexOf(dt) < 0) labels.push(dt);
                    }
                }
                if (LabelNow && channelAddress.channelId.length == 12 && channelAddress.channelId != 'Predict1000h' && channelAddress.channelId.startsWith('Predict10') && channelAddress.channelId.endsWith('h')) {
                    let dataset01: Dataset = datasets.find(label => label.label === 'Prediction 10');
                    let data01: number[] = dataset01.data;
                    let TimeDiff = parseInt(channelAddress.channelId.substring(9, 11), 10);
                    let dt: Date = new Date;
                    dt.setTime(LabelNow.getTime() + TimeDiff * (60 * 60 * 1000));
                    let dtEnd: Date = new Date(this.period.to.getFullYear(), this.period.to.getMonth(), this.period.to.getDate());
                    dtEnd.setDate(dtEnd.getDate() + 1);
                    if (dt < dtEnd) {
                        data01.push(data[indexLastLabel - 1]);
                        if (labels.lastIndexOf(dt) < 0) labels.push(dt);
                    }
                }
                if (LabelNow && channelAddress.channelId.length == 12 && channelAddress.channelId != 'Predict9000h' && channelAddress.channelId.startsWith('Predict90') && channelAddress.channelId.endsWith('h')) {
                    let dataset01: Dataset = datasets.find(label => label.label === 'Prediction 90');
                    let data01: number[] = dataset01.data;
                    let TimeDiff = parseInt(channelAddress.channelId.substring(9, 11), 10);
                    let dt: Date = new Date;
                    dt.setTime(LabelNow.getTime() + TimeDiff * (60 * 60 * 1000));
                    let dtEnd: Date = new Date(this.period.to.getFullYear(), this.period.to.getMonth(), this.period.to.getDate());
                    dtEnd.setDate(dtEnd.getDate() + 1);
                    if (dt < dtEnd) {
                        data01.push(data[indexLastLabel - 1]);
                        if (labels.lastIndexOf(dt) < 0) labels.push(dt);
                    }
                }
            })
            this.labels = labels;
            this.datasets = datasets;
            this.loading = false;

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {

        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ProductionDcActualPower'),
                new ChannelAddress('predictorSolarradiation0', 'Predict00h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict01h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict02h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict03h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict04h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict05h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict06h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict07h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict08h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict09h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict10h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict11h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict12h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1000h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1001h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1002h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1003h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1004h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1005h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1006h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1007h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1008h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1009h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1010h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1011h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict1012h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9000h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9001h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9002h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9003h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9004h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9005h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9006h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9007h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9008h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9009h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9010h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9011h'),
                new ChannelAddress('predictorSolarradiation0', 'Predict9012h'),
            ];
            resolve(result);

        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}