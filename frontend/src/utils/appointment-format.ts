export function appointmentStatus(status: string): string {
  return (
    (
      { confirmado: 'Confirmado', cancelado: 'Cancelado', concluido: 'Concluído' } as Record<
        string,
        string
      >
    )[status] ?? status
  );
}
export function dateLabel(date: string, withYear = true): string {
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    year: withYear ? 'numeric' : undefined,
    month: '2-digit',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(date + 'T12:00:00Z'));
}
export function maskTime(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 4);
  return digits.length > 2 ? digits.slice(0, 2) + ':' + digits.slice(2) : digits;
}
