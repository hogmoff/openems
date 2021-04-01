import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Service, Utils } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { Data, TooltipItem } from '../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Get24HoursPredictionResponse } from "src/app//shared/jsonrpc/response/Get24HoursPredictionResponse";

@Component({
    selector: 'predictionchart',
    templateUrl: '../abstracthistorychart.html'
})
export class PredictionChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

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
        this.spinnerId = "prediction-chart";
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            let datasets = [];

            // required data for self consumption
            let productionData: number[] = [];
            let HistoricPredictionData: number[] = [];
            let HistoricPredictionData10: number[] = [];
            let HistoricPredictionData90: number[] = [];

            if ('_sum/ProductionActivePower' in result.data) {
                /*
                 * Production
                 */
                productionData = result.data['_sum/ProductionActivePower'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
            }
            if ('predictorSolcast0/Predict' in result.data) {
                /*
                 * Prediction
                 */
                HistoricPredictionData = result.data['predictorSolcast0/Predict'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
            }

            if ('predictorSolcast0/Predict10' in result.data) {
                /*
                 * Prediction 10
                 */
                HistoricPredictionData10 = result.data['predictorSolcast0/Predict10'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
            }

            if ('predictorSolcast0/Predict90' in result.data) {
                /*
                 * Prediction 90
                 */
                HistoricPredictionData90 = result.data['predictorSolcast0/Predict90'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
            }

            // Prediction
            this.Get24HoursPredictions().then(response2 => {
                let result2 = (response2 as Get24HoursPredictionResponse).result;
                let predictionData = result2['predictorSolcast0/Predict'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                let predictionData10 = result2['predictorSolcast0/Predict10'].map(value10 => {
                    if (value10 == null) {
                        return null
                    } else {
                        return value10 / 1000; // convert to kW
                    }
                });
                let predictionData90 = result2['predictorSolcast0/Predict90'].map(value90 => {
                    if (value90 == null) {
                        return null
                    } else {
                        return value90 / 1000; // convert to kW
                    }
                });

                if (predictionData.length > 0) {
                    let StartTime = labels.filter(x => x.getTime() >= Date.now())[0];
                    let StartIndex = labels.indexOf(StartTime);

                    let newpredictionData: number[] = new Array(StartIndex - 1);
                    let newlabel = new Date(StartTime.getTime() + 15 * 60000);
                    let prodIndex = 0;
                    for (let i = StartIndex; i < labels.length; i++) {
                        if (labels[i].getTime() >= newlabel.getTime()) {
                            newlabel = new Date(newlabel.getTime() + 15 * 60000);
                            prodIndex++;
                        }
                        newpredictionData.push(predictionData[prodIndex]);
                    }

                    datasets.push({
                        label: '24h ' + this.translate.instant('General.prediction'),
                        data: newpredictionData,
                        hidden: false,
                        borderDash: [5, 5]
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(46,49,49,0.05)',
                        borderColor: 'rgba(46,49,49,0.5)'
                    })
                }
                if (predictionData10.length > 0) {
                    let StartTime = labels.filter(x => x.getTime() >= Date.now())[0];
                    let StartIndex = labels.indexOf(StartTime);

                    let newpredictionData10: number[] = new Array(StartIndex - 1);
                    let newlabel = new Date(StartTime.getTime() + 15 * 60000);
                    let prodIndex = 0;
                    for (let i = StartIndex; i < labels.length; i++) {
                        if (labels[i].getTime() >= newlabel.getTime()) {
                            newlabel = new Date(newlabel.getTime() + 15 * 60000);
                            prodIndex++;
                        }
                        newpredictionData10.push(predictionData10[prodIndex]);
                    }

                    datasets.push({
                        label: '24h ' + this.translate.instant('General.prediction') + ' 10',
                        data: newpredictionData10,
                        hidden: false,
                        borderDash: [5, 5]
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(191,191,191,0.05)',
                        borderColor: 'rgba(191,191,191,0.5)'
                    })
                }
                if (predictionData90.length > 0) {
                    let StartTime = labels.filter(x => x.getTime() >= Date.now())[0];
                    let StartIndex = labels.indexOf(StartTime);

                    let newpredictionData90: number[] = new Array(StartIndex - 1);
                    let newlabel = new Date(StartTime.getTime() + 15 * 60000);
                    let prodIndex = 0;
                    for (let i = StartIndex; i < labels.length; i++) {
                        if (labels[i].getTime() >= newlabel.getTime()) {
                            newlabel = new Date(newlabel.getTime() + 15 * 60000);
                            prodIndex++;
                        }
                        newpredictionData90.push(predictionData90[prodIndex]);
                    }

                    datasets.push({
                        label: '24h ' + this.translate.instant('General.prediction') + ' 90',
                        data: newpredictionData90,
                        hidden: false,
                        borderDash: [5, 5]
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(108,122,137,0.05)',
                        borderColor: 'rgba(108,122,137,0.5)'
                    })
                }
            });

            datasets.push({
                label: this.translate.instant('General.production'),
                data: productionData,
                hidden: false
            })
            this.colors.push({
                backgroundColor: 'rgba(253,197,7,0.05)',
                borderColor: 'rgba(253,197,7,1)',
            });
            datasets.push({
                label: this.translate.instant('General.prediction'),
                data: HistoricPredictionData,
                hidden: false
            })
            this.colors.push({
                backgroundColor: 'rgba(46,49,49,0.05)',
                borderColor: 'rgba(46,49,49,1)'
            })
            datasets.push({
                label: this.translate.instant('General.prediction') + ' 10',
                data: HistoricPredictionData10,
                hidden: false,
                borderDash: [10, 10]
            })
            this.colors.push({
                backgroundColor: 'rgba(191,191,191,0.05)',
                borderColor: 'rgba(191,191,191,1)'
            })
            datasets.push({
                label: this.translate.instant('General.prediction') + ' 90',
                data: HistoricPredictionData90,
                hidden: false,
                borderDash: [10, 10]
            })
            this.colors.push({
                backgroundColor: 'rgba(108,122,137,0.05)',
                borderColor: 'rgba(108,122,137,1)'
            })
            this.service.stopSpinner(this.spinnerId);
            this.datasets = datasets;
            this.loading = false;
        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ProductionActivePower'),
                new ChannelAddress('predictorSolcast0', 'Predict'),
                new ChannelAddress('predictorSolcast0', 'Predict10'),
                new ChannelAddress('predictorSolcast0', 'Predict90')
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}