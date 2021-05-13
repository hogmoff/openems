import { ActivatedRoute } from '@angular/router';
import { SummaryModalComponent } from './modal/modal.component';
import { Component } from '@angular/core';
import { ChannelAddress, Edge, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
  selector: SummaryComponent.SELECTOR,
  templateUrl: './summary.component.html'
})
export class SummaryComponent {

  private static readonly SELECTOR = "summary";

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route)
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: SummaryModalComponent,
    });
    //return await modal.present();
  }
}
