import { getJson, postJson } from '@/services/api';
import { type Contratacao } from '@/services/contracts';
import { type NovoEndereco } from '@/services/addresses';

export type AgendaProfile = 'cliente' | 'prestador';
export type AgendaPeriod = 'proximos' | 'historico' | 'cancelados';
export type Disponibilidade = {
  idDisponibilidade: number;
  data: string;
  hora: string;
  status: string;
};
export type Agendamento = {
  idAgendamento: number;
  idPedido: number;
  idOrcamento: number;
  tituloPedido: string;
  nomeCliente: string;
  nomePrestador: string;
  valor: number;
  data: string;
  hora: string;
  status: string;
  situacaoConversa?: string | null;
  contratacao?: Contratacao | null;
  endereco: NovoEndereco;
};
export type Page<T> = {
  itens: T[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
};
const base = (profile: AgendaProfile) =>
  profile === 'cliente' ? '/clientes/me' : '/prestadores/me';
const id = (value: unknown) => typeof value === 'number' && Number.isInteger(value) && value > 0;
const date = (value: unknown) => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
const time = (value: unknown) =>
  typeof value === 'string' && /^(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(value);
function isSlot(value: unknown): value is Disponibilidade {
  if (!value || typeof value !== 'object') return false;
  const d = value as Disponibilidade;
  return id(d.idDisponibilidade) && date(d.data) && time(d.hora) && typeof d.status === 'string';
}
export function isAppointment(value: unknown): value is Agendamento {
  if (!value || typeof value !== 'object') return false;
  const a = value as Agendamento,
    e = a.endereco;
  return (
    id(a.idAgendamento) &&
    id(a.idPedido) &&
    id(a.idOrcamento) &&
    typeof a.tituloPedido === 'string' &&
    typeof a.nomeCliente === 'string' &&
    typeof a.nomePrestador === 'string' &&
    typeof a.valor === 'number' &&
    Number.isFinite(a.valor) &&
    a.valor > 0 &&
    date(a.data) &&
    time(a.hora) &&
    typeof a.status === 'string' &&
    !!e &&
    ['cep', 'logradouro', 'numero', 'bairro', 'cidade', 'uf'].every(
      (key) => typeof e[key as keyof NovoEndereco] === 'string',
    ) &&
    (e.complemento === null || typeof e.complemento === 'string')
  );
}
function page<T>(value: unknown, item: (v: unknown) => v is T): Page<T> {
  const p = value as Page<T>;
  if (
    !p ||
    !Array.isArray(p.itens) ||
    !p.itens.every(item) ||
    !Number.isInteger(p.pagina) ||
    p.pagina < 0 ||
    !Number.isInteger(p.tamanho) ||
    p.tamanho < 1 ||
    !Number.isInteger(p.totalItens) ||
    p.totalItens < 0 ||
    !Number.isInteger(p.totalPaginas) ||
    p.totalPaginas < 0
  )
    throw new Error('Não foi possível carregar os dados da agenda. Tente novamente.');
  return p;
}
export async function listProviderSlots(token: string, pagina = 0): Promise<Page<Disponibilidade>> {
  return page(
    await getJson('/prestadores/me/disponibilidades?pagina=' + pagina + '&tamanho=10', token),
    isSlot,
  );
}
export async function createSlot(
  token: string,
  data: string,
  hora: string,
): Promise<Disponibilidade> {
  const result = await postJson('/prestadores/me/disponibilidades', { data, hora }, token);
  if (!isSlot(result))
    throw new Error('Confira sua disponibilidade antes de tentar cadastrar o horário novamente.');
  return result;
}
export async function listQuoteSlots(
  token: string,
  orcamento: number,
  pagina = 0,
): Promise<Page<Disponibilidade>> {
  return page(
    await getJson(
      `/clientes/me/orcamentos/${orcamento}/disponibilidades?pagina=${pagina}&tamanho=50`,
      token,
    ),
    isSlot,
  );
}
export async function createAppointment(
  token: string,
  idOrcamento: number,
  idDisponibilidade: number,
  idEndereco: number,
): Promise<Agendamento> {
  const result = await postJson(
    '/clientes/me/agendamentos',
    { idOrcamento, idDisponibilidade, idEndereco },
    token,
  );
  if (!isAppointment(result))
    throw new Error('Confira sua Agenda antes de tentar agendar novamente.');
  return result;
}
export async function listAppointments(
  token: string,
  profile: AgendaProfile,
  periodo: AgendaPeriod,
  pagina = 0,
): Promise<Page<Agendamento>> {
  return page(
    await getJson(
      `${base(profile)}/agendamentos?periodo=${periodo}&pagina=${pagina}&tamanho=10`,
      token,
    ),
    isAppointment,
  );
}
export async function getAppointment(
  token: string,
  profile: AgendaProfile,
  appointment: number,
): Promise<Agendamento> {
  const result = await getJson(`${base(profile)}/agendamentos/${appointment}`, token);
  if (!isAppointment(result))
    throw new Error('Não foi possível carregar o agendamento. Tente novamente.');
  return result;
}
