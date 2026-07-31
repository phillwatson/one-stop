export function getDefaultLocale(): Intl.Locale | undefined {
  const dateTimeOptions = Intl.DateTimeFormat().resolvedOptions();
  if (dateTimeOptions) {
    const localeTag = dateTimeOptions.locale;
    if (localeTag) {
      return new Intl.Locale(localeTag);
    }
  }

  return undefined;
}

export function getDefaultLocaleRegion(): string {
  const locale = getDefaultLocale();
  return locale?.region || 'GB';
}

export const defaultLocale = getDefaultLocale()?.baseName || 'en-GB';

export function toDate(dateStr?: string): Date | undefined {
  if (!dateStr) return undefined;
  return new Date(dateStr);
}

export function formatDate(dateStr?: string | Date): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleDateString(defaultLocale);
}

const shortDateOptions: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "short",
  day: "numeric"
}
const shortDateOptionsWithoutDay: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "short"
}
export function formatShortDate(dateStr?: string | Date, withoutDay: boolean = false): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleDateString(defaultLocale, withoutDay ? shortDateOptionsWithoutDay : shortDateOptions);
}

export function formatTime(dateStr?: string | Date): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleTimeString(defaultLocale);
}

export function formatDateTime(dateStr?: string | Date): string {
  if (!dateStr) return "";
  return formatDate(dateStr) + ' ' + formatTime(dateStr);
}

export function toLocaleDate(date: Date): string {
  if (!date) return "";
  return date.toLocaleDateString(defaultLocale);
}

export function toLocalDateTime(date: Date): string {
  if (!date) return "";
  return date.toLocaleString(defaultLocale);
}

export function toISODate(date: Date): string {
  return date.toISOString().substring(0, 10);
}

export function minDate(dateA: Date, dateB: Date): Date {
  return (dateA)
    ? (dateB)
      ? (dateA < dateB) ? dateA : dateB
      : dateA
    : dateB;
}

export function maxDate(dateA: Date, dateB: Date): Date {
  return (dateA)
    ? (dateB)
      ? (dateA > dateB) ? dateA : dateB
      : dateA
    : dateB;
}

export function startOfDay(date: Date, plusDays: number = 0): Date {
  const result = new Date(date);
  result.setHours(0, 0, 0, 0);

  if (plusDays !== 0) {
    result.setDate(result.getDate() + plusDays);
  }

  return result;
}

/**
 * Calculates the start of the month for the given date, with the time set to 00:00:00.000.
 * 
 * @param date the date for which the start of the month is to be returned.
 * @param plusMonth on optional number of months to add to the date before returning the start of the month.
 * @returns the start of the month for the given date, with the time set to 00:00:00.000.
 */
export function startOfMonth(date: Date, plusMonth: number = 0): Date {
  const result = startOfDay(date);
  result.setDate(1);

  if (plusMonth !== 0) {
    result.setMonth(result.getMonth() + plusMonth);
  }

  return result;
}
