import { getJson, postJson } from '@/services/api';
import { isAppointment, type Agendamento } from '@/services/appointments';
export type Contratacao = {
  idContratacao: number;
  idAgendamento: number;
  idPedido: number;
  valor: number;
  confirmadaEm: string;
};
export type RevisaoContratacao = {
  agendamento: Agendamento;
  podeConfirmar: boolean;
  motivoBloqueio: string | null;
};
export function isContract(value: unknown): value is Contratacao {
  if (!value || typeof value !== 'object') return false;
  const c = value as Contratacao;
  return (
    Number.isInteger(c.idContratacao) &&
    c.idContratacao > 0 &&
    Number.isInteger(c.idAgendamento) &&
    c.idAgendamento > 0 &&
    Number.isInteger(c.idPedido) &&
    c.idPedido > 0 &&
    typeof c.valor === 'number' &&
    Number.isFinite(c.valor) &&
    c.valor > 0 &&
    typeof c.confirmadaEm === 'string' &&
    !Number.isNaN(Date.parse(c.confirmadaEm))
  );
}
const path = (id: number) => `/clientes/me/agendamentos/${id}/contratacao`;
export async function reviewContract(token: string, id: number): Promise<RevisaoContratacao> {
  const r = await getJson<RevisaoContratacao>(path(id) + '/revisao', token);
  if (
    !r ||
    !isAppointment(r.agendamento) ||
    typeof r.podeConfirmar !== 'boolean' ||
    !(r.motivoBloqueio === null || typeof r.motivoBloqueio === 'string')
  )
    throw new Error('Não foi possível carregar a revisão da contratação.');
  return r;
}
export async function confirmContract(token: string, id: number): Promise<Contratacao> {
  const c = await postJson<unknown>(path(id), {}, token);
  if (!isContract(c))
    throw new Error('Confira os detalhes do agendamento antes de tentar confirmar novamente.');
  return c;
}
