import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
  selector: SummaryModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SummaryModalComponent {

  private static readonly SELECTOR = "summary-modal";

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
  ) { }
}