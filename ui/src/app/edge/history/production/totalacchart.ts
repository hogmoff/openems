import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'productionTotalAcChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionTotalAcChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() private showPhases: boolean;

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
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
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
                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach(channelAddress => {
                            let data = result.data[channelAddress.toString()].map(value => {
                                if (value == null) {
                                    return null
                                } else {
                                    return value / 1000; // convert to kW
                                }
                            });
                            if (!data) {
                                return;
                            } else {
                                if (channelAddress.channelId == 'ProductionAcActivePower') {
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
                                        label: 'Vorhersage',
                                        borderDash: [10, 10],
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(45,143,171,0.05)',
                                        borderColor: 'rgba(45,143,171,1)'
                                    });
                                }
                                if (channelAddress.channelId == 'Predict1000h') {
                                    datasets.push({
                                        label: 'Vorhersage 10',
                                        borderDash: [10, 10],
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(100,100,100,0.05)',
                                        borderColor: 'rgba(100,100,100,1)'
                                    });
                                }
                                if (channelAddress.channelId == 'Predict9000h') {
                                    datasets.push({
                                        label: 'Vorhersage 90',
                                        borderDash: [10, 10],
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(170,170,170,0.05)',
                                        borderColor: 'rgba(170,170,170,1)'
                                    });
                                }
                                if (LabelNow && channelAddress.channelId.length == 10 && channelAddress.channelId != 'Predict00h' && channelAddress.channelId.startsWith('Predict') && channelAddress.channelId.endsWith('h')) {
                                    let dataset01: Dataset = datasets.find(label => label.label === 'Vorhersage');
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
                                    let dataset01: Dataset = datasets.find(label => label.label === 'Vorhersage 10');
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
                                    let dataset01: Dataset = datasets.find(label => label.label === 'Vorhersage 90');
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
                                if ('_sum/ProductionAcActivePowerL1' && '_sum/ProductionAcActivePowerL2' && '_sum/ProductionAcActivePowerL3' in result.data && this.showPhases == true) {
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL1') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: data
                                        });
                                        this.colors.push(this.phase1Color);
                                    }
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: data
                                        });
                                        this.colors.push(this.phase2Color);
                                    }
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: data
                                        });
                                        this.colors.push(this.phase3Color);
                                    }
                                }
                            }
                        });
                    });
                    this.labels = labels;
                    this.datasets = datasets;
                    this.loading = false;
                }).catch(reason => {
                    console.error(reason); // TODO error message
                    this.initializeChart();
                    return;
                });
            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
            });
        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {

        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ProductionActivePower'),
                new ChannelAddress('_sum', 'ProductionAcActivePower'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
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