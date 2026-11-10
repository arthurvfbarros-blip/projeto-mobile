export type OrderDraft = {
  idCategoria: number | null;
  titulo: string;
  descricao: string;
  idEndereco: number | null;
  dataDesejada: string;
};

export type OrderErrors = Partial<Record<keyof OrderDraft, string>>;

export function todayInBrazil(): string {
  const parts = new Intl.DateTimeFormat('pt-BR', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
}

export function dateToIso(value: string): string | null {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value.trim());
  if (!match) return null;
  const [, day, month, year] = match;
  const iso = `${year}-${month}-${day}`;
  const date = new Date(`${iso}T12:00:00Z`);
  if (Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== iso || Number(year) === 0)
    return null;
  return iso;
}

export function formatOrderDate(value: string | null): string {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return 'Não informada';
  const [year, month, day] = value.split('-');
  return `${day}/${month}/${year}`;
}

export function formatCreatedAt(value: string | null): string {
  if (!value) return 'Não informada';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Não informada';
  return date.toLocaleDateString('pt-BR', { timeZone: 'America/Sao_Paulo' });
}

export function orderStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    aberto: 'Aberto',
    em_orcamento: 'Recebendo orçamentos',
    aguardando_confirmacao: 'Aguardando confirmação',
    agendado: 'Agendado',
    concluido: 'Concluído',
    cancelado: 'Cancelado',
  };
  return labels[status] ?? 'Em andamento';
}

export function validateOrder(draft: OrderDraft): OrderErrors {
  const date = dateToIso(draft.dataDesejada);
  const title = draft.titulo.trim();
  const description = draft.descricao.trim();
  return {
    idCategoria: draft.idCategoria ? undefined : 'Selecione uma categoria',
    titulo: !title
      ? 'Informe o título do pedido'
      : title.length > 150
        ? 'O título deve ter no máximo 150 caracteres'
        : undefined,
    descricao:
      description.length < 20 || description.length > 5000
        ? 'A descrição deve ter entre 20 e 5000 caracteres'
        : undefined,
    idEndereco: draft.idEndereco ? undefined : 'Selecione um endereço',
    dataDesejada: !date
      ? 'Informe uma data válida no formato DD/MM/AAAA'
      : date < todayInBrazil()
        ? 'A data desejada não pode estar no passado'
        : undefined,
  };
}
