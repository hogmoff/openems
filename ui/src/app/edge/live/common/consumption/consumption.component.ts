import { ChannelAddress, CurrentData, EdgeConfig, Utils } from '../../../../shared/shared';
import { ConsumptionModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { Component } from '@angular/core';

@Component({
  selector: 'consumption',
  templateUrl: './consumption.component.html'
})
export class ConsumptionComponent extends AbstractFlatWidget {

  public evcss: EdgeConfig.Component[] | null = null;
  public consumptionMeters: EdgeConfig.Component[] = null;
  public GoodweComponents: EdgeConfig.Component[] = null;
  public sumActivePower: number = 0;
  public sumActivePowerHousehold1: number = 0;
  public sumActivePowerWithoutBackup: number = 0;
  public sumActiveBackupPower: number = 0;
  public evcsSumOfChargePower: number;
  public otherPower: number;
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  protected getChannelAddresses() {

    let channelAddresses: ChannelAddress[] = [
      new ChannelAddress('_sum', 'ConsumptionActivePower'),

      // TODO should be moved to Modal
      new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
      new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
      new ChannelAddress('_sum', 'ConsumptionActivePowerL3')
    ]

    // Get consumptionMeterComponents
    this.consumptionMeters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
      .filter(component => component.properties['type'] == 'CONSUMPTION_METERED' && !component.factoryId.startsWith('GoodWe'));

    for (let component of this.consumptionMeters) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ActivePower'),
      )
    }

    // Goodwe Channels
    this.GoodweComponents = this.config.getComponentsImplementingNature("io.openems.edge.goodwe.common.GoodWe").filter(component => component.isEnabled == true);
    for (let component of this.GoodweComponents) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'BackUpPLoadR'),
        new ChannelAddress(component.id, 'BackUpPLoadS'),
        new ChannelAddress(component.id, 'BackUpPLoadT'),
        new ChannelAddress(component.id, 'TotalBackUpLoad'),
      )
    }

    // Get EVCSs
    this.evcss = this.config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
        !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);

    for (let component of this.evcss) {
      channelAddresses.push(
        new ChannelAddress(component.id, 'ChargePower'),
      )
    }

    console.log(channelAddresses);
    return channelAddresses;
  }

  protected onCurrentData(currentData: CurrentData) {

    //console.log(currentData);
    this.evcsSumOfChargePower = 0;
    this.sumActivePowerWithoutBackup = 0;
    this.sumActiveBackupPower = 0;
    this.sumActivePowerHousehold1 = 0;
    let consumptionMetersSumOfActivePower: number = 0;
    this.sumActivePower = currentData.allComponents['_sum/ConsumptionActivePower'];

    // Goodwe Backup
    for (let component of this.GoodweComponents) {
      let goodweChannel: ChannelAddress = new ChannelAddress(component.id, 'TotalBackUpLoad');
      if (currentData.allComponents[component.id + '/TotalBackUpLoad']) {
        this.sumActiveBackupPower = currentData.allComponents[goodweChannel.toString()];
      }
    }
    this.sumActivePowerWithoutBackup = Utils.subtractSafely(this.sumActivePower, this.sumActiveBackupPower);


    // TODO move sums to Model
    // Iterate over evcsComponents to get ChargePower for every component
    for (let component of this.evcss) {
      if (currentData.allComponents[component.id + '/ChargePower']) {
        this.evcsSumOfChargePower += currentData.allComponents[component.id + '/ChargePower'];
      }
    }

    // Iterate over GoodWeComponents to get ChargePower for every component
    for (let component of this.consumptionMeters) {
      if (currentData.allComponents[component.id + '/ActivePower']) {
        consumptionMetersSumOfActivePower += currentData.allComponents[component.id + '/ActivePower'];
      }
    }

    this.otherPower = Utils.subtractSafely(this.sumActivePower,
      Utils.addSafely(this.evcsSumOfChargePower, consumptionMetersSumOfActivePower));

    this.sumActivePowerHousehold1 = this.otherPower - this.sumActiveBackupPower;
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ConsumptionModalComponent,
      componentProps: {
        edge: this.edge,
        evcss: this.evcss,
        consumptionMeters: this.consumptionMeters,
        GoodweComponents: this.GoodweComponents,
        evcsSumOfChargePower: this.evcsSumOfChargePower,
        sumActivePowerWithoutBackup: this.sumActivePowerWithoutBackup,
        sumActivePowerHousehold1: this.sumActivePowerHousehold1,
        sumActiveBackupPower: this.sumActiveBackupPower,
        otherPower: this.otherPower,
      }
    });
    return await modal.present();
  }
}