import { ActivatedRoute } from '@angular/router';
import { SummaryTextModalComponent } from './modal/modal.component';
import { Component } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
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
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route)
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: SummaryTextModalComponent,
    });
    return await modal.present();
  }
}
