import { maxDate, minDate, startOfDay, startOfMonth } from './date-util';

describe('minDate', () => {
  it('returns the earlier date when both dates are provided', () => {
    const earlier = new Date(2024, 0, 10, 12, 0, 0, 0);
    const later = new Date(2024, 0, 12, 12, 0, 0, 0);

    expect(minDate(later, earlier)).toEqual(earlier);
  });

  it('returns the defined date when only one date is provided', () => {
    const date = new Date(2024, 0, 10, 12, 0, 0, 0);

    expect(minDate(date, undefined as unknown as Date)).toEqual(date);
    expect(minDate(undefined as unknown as Date, date)).toEqual(date);
  });
});

describe('maxDate', () => {
  it('returns the later date when both dates are provided', () => {
    const earlier = new Date(2024, 0, 10, 12, 0, 0, 0);
    const later = new Date(2024, 0, 12, 12, 0, 0, 0);

    expect(maxDate(earlier, later)).toEqual(later);
  });

  it('returns the defined date when only one date is provided', () => {
    const date = new Date(2024, 0, 10, 12, 0, 0, 0);

    expect(maxDate(date, undefined as unknown as Date)).toEqual(date);
    expect(maxDate(undefined as unknown as Date, date)).toEqual(date);
  });
});

describe('startOfDay', () => {
  it('returns the start of the same day with the time set to midnight', () => {
    const input = new Date(2024, 4, 15, 13, 45, 30, 123);
    const expected = new Date(2024, 4, 15, 0, 0, 0, 0);

    expect(startOfDay(input)).toEqual(expected);
  });

  it('adds the requested number of days from midnight of the current day', () => {
    const input = new Date(2024, 4, 15, 13, 45, 30, 123);
    const expected = new Date(2024, 4, 17, 0, 0, 0, 0);

    expect(startOfDay(input, 2)).toEqual(expected);
  });

  it('handles month boundaries when adding days', () => {
    const input = new Date(2024, 0, 31, 23, 59, 59, 999);
    const expected = new Date(2024, 1, 1, 0, 0, 0, 0);

    expect(startOfDay(input, 1)).toEqual(expected);
  });
});

describe('startOfMonth', () => {
  for (let month = 0; month < 12; month += 1) {
    it(`returns the first day of the following month for month ${month + 1}`, () => {
      const input = new Date(2024, month, 15, 12, 34, 56, 789);
      const expected = new Date(2024, month + 1, 1, 0, 0, 0, 0);

      expect(startOfMonth(input, 1)).toEqual(expected);
    });
  }
});
