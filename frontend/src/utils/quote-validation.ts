export function parseQuoteValue(value: string): number | null {
  const clean = value.trim().replace(/^R\$\s*/, '');
  if (!/^(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2})?$/.test(clean)) return null;
  const parsed = Number(clean.replace(/\./g, '').replace(',', '.'));
  return Number.isFinite(parsed) && parsed >= 0.01 && parsed <= 99_999_999.99 ? parsed : null;
}

export function formatMoney(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
}

export function quoteStatusLabel(status: string): string {
  return (
    (
      {
        enviado: 'Enviado',
        aceito: 'Aceito',
        recusado: 'Recusado',
        expirado: 'Expirado',
      } as Record<string, string>
    )[status] ?? status
  );
}

export function formatDeadline(days: number | null): string {
  return days === null ? 'Prazo não informado' : `${days} ${days === 1 ? 'dia' : 'dias'}`;
}

export function validRequestId(id: string | undefined): number | null {
  const parsed = Number(id);
  return /^\d+$/.test(id ?? '') && Number.isInteger(parsed) && parsed > 0 && parsed <= 2_147_483_647
    ? parsed
    : null;
}
