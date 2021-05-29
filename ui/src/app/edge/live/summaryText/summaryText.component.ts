import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { SummaryTextModalComponent } from './modal/modal.component';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  selector: SummaryTextComponent.SELECTOR,
  templateUrl: './summaryText.component.html'
})
export class SummaryTextComponent {

  private static readonly SELECTOR = "summaryText";

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      let channels = [];
      // general consumption channels
      channels.push(
        new ChannelAddress('summary0', 'DailyProduction'),
        new ChannelAddress('summary0', 'MonthlyProduction'),
        new ChannelAddress('summary0', 'YearlyProduction'),
        new ChannelAddress('summary0', 'SumProduction'),
        new ChannelAddress('summary0', 'DailyConsumption'),
        new ChannelAddress('summary0', 'DailySell'),
        new ChannelAddress('summary0', 'DailyBuy'),
      )
      this.edge.subscribeChannels(this.websocket, SummaryTextComponent.SELECTOR, channels);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, SummaryTextComponent.SELECTOR);
    }
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: SummaryTextModalComponent,
      componentProps: {
        edge: this.edge,
      }
    });
    return await modal.present();
  }
}
