import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats with two decimals and thousands separators', () => {
    expect(pipe.transform(1654.456)).toBe('$1,654.46');
  });

  it('returns blank for null, undefined and NaN', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform(NaN)).toBe('');
  });

  it('prefixes negatives with a minus sign', () => {
    expect(pipe.transform(-1234.5)).toBe('-$1,234.50');
  });

  it('adds a plus sign only when showSign is requested', () => {
    expect(pipe.transform(42, true)).toBe('+$42.00');
    expect(pipe.transform(42)).toBe('$42.00');
    expect(pipe.transform(-42, true)).toBe('-$42.00');
  });

  it('formats zero as $0.00 (with a plus when showSign is requested)', () => {
    expect(pipe.transform(0)).toBe('$0.00');
    expect(pipe.transform(0, true)).toBe('+$0.00');
  });
});
