import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats with two decimals and thousands separators', () => {
    expect(pipe.transform(1654.456)).toBe('$1,654.46');
  });

  it('returns blank for null', () => {
    expect(pipe.transform(null)).toBe('');
  });
});
