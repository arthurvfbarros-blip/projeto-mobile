import 'fast-text-encoding';
import { Client } from '@stomp/stompjs';
import { API_URL, getJson, postJson } from '@/services/api';
import { type NovoEndereco } from '@/services/addresses';

export type ContatoChat = { nome: string; telefone: string | null };
export type ChatContext = {
  idAgendamento: number;
  tituloPedido: string;
  situacaoConversa: string;
  cliente: ContatoChat;
  prestador: ContatoChat;
  endereco: NovoEndereco;
  idUsuarioAtual: number;
};
export type Mensagem = {
  idMensagem: number;
  idAgendamento: number;
  idEnvio: string;
  idRemetente: number;
  tipoRemetente: string;
  nomeRemetente: string;
  conteudo: string;
  criadoEm: string;
};
export type PaginaMensagens = { itens: Mensagem[]; temMais: boolean; proximoCursor: number | null };
const path = (id: number) => `/agendamentos/${id}/chat`;
const positive = (id: unknown) => typeof id === 'number' && Number.isInteger(id) && id > 0;
export function isMessage(value: unknown): value is Mensagem {
  if (!value || typeof value !== 'object') return false;
  const m = value as Mensagem;
  return (
    positive(m.idMensagem) &&
    positive(m.idAgendamento) &&
    positive(m.idRemetente) &&
    typeof m.idEnvio === 'string' &&
    typeof m.tipoRemetente === 'string' &&
    typeof m.nomeRemetente === 'string' &&
    typeof m.conteudo === 'string' &&
    typeof m.criadoEm === 'string' &&
    !Number.isNaN(Date.parse(m.criadoEm))
  );
}
function isContact(value: unknown): value is ContatoChat {
  if (!value || typeof value !== 'object') return false;
  const c = value as ContatoChat;
  return typeof c.nome === 'string' && (c.telefone === null || typeof c.telefone === 'string');
}
export async function getChat(token: string, id: number): Promise<ChatContext> {
  const c = await getJson<ChatContext>(path(id), token),
    e = c?.endereco;
  if (
    !c ||
    !positive(c.idAgendamento) ||
    !positive(c.idUsuarioAtual) ||
    typeof c.tituloPedido !== 'string' ||
    typeof c.situacaoConversa !== 'string' ||
    !isContact(c.cliente) ||
    !isContact(c.prestador) ||
    !e ||
    !['cep', 'logradouro', 'numero', 'bairro', 'cidade', 'uf'].every(
      (k) => typeof e[k as keyof NovoEndereco] === 'string',
    ) ||
    !(e.complemento === null || typeof e.complemento === 'string')
  )
    throw new Error('Não foi possível carregar a conversa. Tente novamente.');
  return c;
}
export async function getMessages(
  token: string,
  id: number,
  cursor?: { antesDe: number } | { depoisDe: number },
): Promise<PaginaMensagens> {
  const query = cursor
    ? '&' +
      new URLSearchParams(
        Object.fromEntries(Object.entries(cursor).map(([k, v]) => [k, String(v)])),
      ).toString()
    : '';
  const p = await getJson<PaginaMensagens>(path(id) + '/mensagens?tamanho=30' + query, token);
  if (
    !p ||
    !Array.isArray(p.itens) ||
    !p.itens.every(isMessage) ||
    typeof p.temMais !== 'boolean' ||
    !(p.proximoCursor === null || positive(p.proximoCursor))
  )
    throw new Error('Não foi possível carregar as mensagens. Tente novamente.');
  return p;
}
export async function sendMessage(
  token: string,
  id: number,
  idEnvio: string,
  conteudo: string,
): Promise<Mensagem> {
  const m = await postJson<unknown>(path(id) + '/mensagens', { idEnvio, conteudo }, token);
  if (!isMessage(m)) throw new Error('Não foi possível confirmar o envio. Tente novamente.');
  return m;
}
export function connectChat(
  token: string,
  id: number,
  callbacks: {
    onConnected: () => void;
    onDisconnected: () => void;
    onMessage: (m: Mensagem) => void;
    onError: () => void;
  },
): Client {
  const client = new Client({
    brokerURL: API_URL.replace(/^http/, 'ws') + '/ws-chat',
    connectHeaders: { Authorization: 'Bearer ' + token },
    reconnectDelay: 5000,
    connectionTimeout: 10000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      client.subscribe(`/topic/agendamentos/${id}/mensagens`, (event) => {
        try {
          const m: unknown = JSON.parse(event.body);
          if (isMessage(m) && m.idAgendamento === id) callbacks.onMessage(m);
        } catch {
          callbacks.onError();
        }
      });
      callbacks.onConnected();
    },
    onWebSocketClose: callbacks.onDisconnected,
    onWebSocketError: callbacks.onError,
    onStompError: callbacks.onError,
  });
  client.activate();
  return client;
}
