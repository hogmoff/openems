import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Service, Edge } from '../../../../shared/shared';

@Component({
    selector: PredictionChartOverviewComponent.SELECTOR,
    templateUrl: './predictionchartoverview.component.html'
})
export class PredictionChartOverviewComponent {

    private static readonly SELECTOR = "prediction-chart-overview";

    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }
}