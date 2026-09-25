import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [AppComponent]
    });
  });

  it('renders the brand and one nav link per seeded account plus "New transfer"', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('.brand')!.textContent).toBe('Westlake Advisor Workstation');
    const links = Array.from(el.querySelectorAll('nav a'));
    expect(links.map(a => a.textContent!.trim())).toEqual(['7781-2204', '7781-9930', 'New transfer']);
    expect(links[0].getAttribute('href')).toBe('/accounts/7781-2204/positions');
    expect(links[2].getAttribute('href')).toBe('/transfers/new');
  });
});
