import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ConsumptionModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
  selector: ConsumptionComponent.SELECTOR,
  templateUrl: './consumption.component.html'
})
export class ConsumptionComponent {

  private static readonly SELECTOR = "consumption";

  public config: EdgeConfig = null;
  public edge: Edge = null;
  public evcsComponents: EdgeConfig.Component[] = null;
  public consumptionMeterComponents: EdgeConfig.Component[] = null;

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
        new ChannelAddress('_sum', 'ConsumptionActivePower'),
        // channels for modal component, subscribe here for better UX
        new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
        new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
        new ChannelAddress('_sum', 'ConsumptionActivePowerL3'),
      )
      // other consumption channels
      this.service.getConfig().then(config => {
        this.config = config;
        this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.properties['type'] == 'CONSUMPTION_METERED');
        for (let component of this.consumptionMeterComponents) {
          channels.push(
            new ChannelAddress(component.id, 'ActivePower'),
          )
        }
        this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') && !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);
        for (let component of this.evcsComponents) {
          channels.push(
            new ChannelAddress(component.id, 'ChargePower'),
          )
        }
        // Goodwe Backup Channels
        channels.push(new ChannelAddress('ess0', 'BackUpPLoadR'))
        channels.push(new ChannelAddress('ess0', 'BackUpPLoadS'))
        channels.push(new ChannelAddress('ess0', 'BackUpPLoadT'))
      })
      this.edge.subscribeChannels(this.websocket, ConsumptionComponent.SELECTOR, channels);
    });
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: ConsumptionModalComponent,
      componentProps: {
        edge: this.edge,
        evcsComponents: this.evcsComponents,
        consumptionMeterComponents: this.consumptionMeterComponents,
        currentTotalChargingPower: this.currentTotalChargingPower,
        currentTotalConsumptionMeterPower: this.currentTotalConsumptionMeterPower,
        sumOfChannel: this.sumOfChannel,
        getTotalOtherPower: this.getTotalOtherPower,
      }
    });
    return await modal.present();
  }

  public getTotalOtherPower(): number {
    return this.currentTotalChargingPower() + this.currentTotalConsumptionMeterPower();
  }

  private currentTotalChargingPower(): number {
    return this.sumOfChannel(this.evcsComponents, "ChargePower");
  }

  private currentTotalConsumptionMeterPower(): number {
    return this.sumOfChannel(this.consumptionMeterComponents, "ActivePower");
  }

  private sumOfChannel(components: EdgeConfig.Component[], channel: String): number {
    let sum = 0;
    components.forEach(component => {
      let channelValue = this.edge.currentData.value.channel[component.id + "/" + channel];
      if (channelValue != null) {
        sum += channelValue;
      };
    });
    return sum;
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ConsumptionComponent.SELECTOR);
    }
  }
}

