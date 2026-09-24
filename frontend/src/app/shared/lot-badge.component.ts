import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-lot-badge',
  template: `<span class="badge" [class.many]="lots > 3">{{ label }}</span>`,
  standalone: true
})
export class LotBadgeComponent {
  @Input() lots = 0;

  get label(): string {
    if (this.lots === 0) { return 'no lots'; }
    if (this.lots === 1) { return '1 lot'; }
    return this.lots + ' lots';
  }
}
