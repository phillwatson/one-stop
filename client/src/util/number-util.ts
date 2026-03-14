
/**
 * A number formatter to format numbers as percentages.
 */
const percentFormat = new Intl.NumberFormat(undefined, {
    style: 'percent',
    minimumSignificantDigits: 1,
    maximumSignificantDigits: 3,
  });

/**
 * Formats a number as a percentage string.
 * @param value the number to format.
 * @returns the formatted percentage string.
 */
export function percentageFormatter(value: number | null): string {
  return value === null ? '' : percentFormat.format(value / 100);
}
