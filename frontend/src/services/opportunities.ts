import { ApiError, getJson } from '@/services/api';
import {
  isOrderSummary,
  isOrdersPage,
  type PaginaPedidos,
  type PedidoResumo,
} from '@/services/orders';

export type Oportunidade = PedidoResumo & { descricao: string };

const PATH = '/prestadores/me/oportunidades';

export async function listOpportunities(accessToken: string, pagina = 0): Promise<PaginaPedidos> {
  const response = await getJson<unknown>(`${PATH}?pagina=${pagina}&tamanho=10`, accessToken);
  if (!isOrdersPage(response)) {
    throw new Error('Não foi possível carregar as oportunidades. Tente novamente.');
  }
  return response;
}

export async function getOpportunity(accessToken: string, idPedido: number): Promise<Oportunidade> {
  const response = await getJson<unknown>(`${PATH}/${idPedido}`, accessToken);
  if (!isOrderSummary(response) || typeof (response as Oportunidade).descricao !== 'string') {
    throw new Error('Não foi possível carregar a oportunidade. Tente novamente.');
  }
  return response as Oportunidade;
}

export function opportunityError(failure: unknown): string {
  if (failure instanceof ApiError && failure.status === 403) {
    return 'Seu cadastro precisa estar aprovado e ativo para consultar oportunidades.';
  }
  if (failure instanceof ApiError && failure.status === 400) {
    const fields = Object.values(failure.fields).filter((value) => typeof value === 'string');
    return fields.length ? fields.join('. ') : failure.message;
  }
  return failure instanceof Error
    ? failure.message
    : 'Não foi possível consultar as oportunidades.';
}
