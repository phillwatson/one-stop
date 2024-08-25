
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

export function getDefaultLocaleRegion(): string | undefined {
  const locale = getDefaultLocale();
  return (locale) ? locale.region : undefined;
}

export function formatDate(dateStr?: string): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleDateString("en-GB");
}

export function formatTime(dateStr?: string): string {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleTimeString("en-GB");
}

export function formatDateTime(dateStr?: string): string {
  if (!dateStr) return "";
  return formatDate(dateStr) + ' ' + formatTime(dateStr);
}

export function toLocaleDate(date: Date): string {
  if (!date) return "";
  return date.toLocaleDateString("en-GB");
}

export function toISODate(date: Date): string {
  return date.toISOString().substring(0, 10);
}