import { getJson, postJson } from '@/services/api';

export type Orcamento = {
  idOrcamento: number;
  idPedido: number;
  tituloPedido: string;
  prestador: { idPrestador: number; nome: string };
  valor: number;
  prazoDias: number | null;
  descricao: string | null;
  status: string;
};
export type NovoOrcamento = {
  valor: number;
  prazoDias: number;
  descricao: string | null;
};
export type PaginaOrcamentos = {
  itens: Orcamento[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
};

function isQuote(value: unknown): value is Orcamento {
  if (!value || typeof value !== 'object') return false;
  const q = value as Orcamento;
  return (
    Number.isInteger(q.idOrcamento) &&
    q.idOrcamento > 0 &&
    Number.isInteger(q.idPedido) &&
    q.idPedido > 0 &&
    typeof q.tituloPedido === 'string' &&
    !!q.prestador &&
    Number.isInteger(q.prestador.idPrestador) &&
    q.prestador.idPrestador > 0 &&
    typeof q.prestador.nome === 'string' &&
    typeof q.valor === 'number' &&
    Number.isFinite(q.valor) &&
    q.valor > 0 &&
    (q.prazoDias === null || (Number.isInteger(q.prazoDias) && q.prazoDias > 0)) &&
    (q.descricao === null || typeof q.descricao === 'string') &&
    typeof q.status === 'string'
  );
}

const providerPath = (idPedido: number) => `/prestadores/me/oportunidades/${idPedido}/orcamento`;

export async function getMyQuote(token: string, idPedido: number): Promise<Orcamento> {
  const result = await getJson<unknown>(providerPath(idPedido), token);
  if (!isQuote(result)) throw new Error('Não foi possível carregar o orçamento. Tente novamente.');
  return result;
}

export async function createQuote(
  token: string,
  idPedido: number,
  data: NovoOrcamento,
): Promise<Orcamento> {
  const result = await postJson<unknown>(providerPath(idPedido), data, token);
  if (!isQuote(result))
    throw new Error('Confira se o orçamento foi enviado antes de tentar novamente.');
  return result;
}

export async function listQuotes(
  token: string,
  idPedido: number,
  pagina = 0,
): Promise<PaginaOrcamentos> {
  const result = await getJson<unknown>(
    `/clientes/me/pedidos/${idPedido}/orcamentos?pagina=${pagina}&tamanho=10`,
    token,
  );
  const p = result as PaginaOrcamentos;
  if (
    !p ||
    !Array.isArray(p.itens) ||
    !p.itens.every(isQuote) ||
    !Number.isInteger(p.pagina) ||
    p.pagina < 0 ||
    !Number.isInteger(p.tamanho) ||
    p.tamanho < 1 ||
    !Number.isInteger(p.totalItens) ||
    p.totalItens < 0 ||
    !Number.isInteger(p.totalPaginas) ||
    p.totalPaginas < 0
  ) {
    throw new Error('Não foi possível carregar os orçamentos. Tente novamente.');
  }
  return p;
}
