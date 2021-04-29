import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
  selector: SummaryTextModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SummaryTextModalComponent {

  private static readonly SELECTOR = "summaryText-modal";

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
  ) { }
}