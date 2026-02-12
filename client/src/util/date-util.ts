
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

export function formatDate(dateStr?: string): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleDateString(defaultLocale);
}

export function formatTime(dateStr?: string): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleTimeString(defaultLocale);
}

export function formatDateTime(dateStr?: string): string {
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
  const year = date.getFullYear();
  const month = date.getMonth();
  const day = date.getDate();

  let result = new Date();
  result.setUTCHours(0);
  result.setUTCMinutes(0);
  result.setUTCSeconds(0);
  result.setUTCMilliseconds(0);
  result.setUTCFullYear(year);
  result.setUTCMonth(month);
  result.setUTCDate(day);

  if (plusDays > 0)
    result.setDate(plusDays);
  return result;
}

export function startOfMonth(date: Date, plusMonth: number = 0): Date {
  let result = startOfDay(date);
  result.setDate(1);
  if (plusMonth > 0)
    result.setMonth(result.getMonth() + plusMonth);
  return result;
}
