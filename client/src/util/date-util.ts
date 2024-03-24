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
