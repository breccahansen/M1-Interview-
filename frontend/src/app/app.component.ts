import { Component } from '@angular/core';
import { NgFor } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: true,
  imports: [NgFor, RouterLink, RouterLinkActive, RouterOutlet]
})
export class AppComponent {
  title = 'Westlake Advisor Workstation';
  accounts = ['7781-2204', '7781-9930'];
}
