import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet]
})
export class AppComponent {
  title = 'Westlake Advisor Workstation';
  accounts = ['7781-2204', '7781-9930'];
}
