import { getJson, postJson } from '@/services/api';
import { connectRealtime, type RealtimeCallbacks } from '@/services/realtime';
import { type Page } from '@/services/appointments';
export type Notificacao = {
  idNotificacao: number;
  idAgendamento: number;
  tipo: string;
  mensagem: string;
  criadaEm: string;
  lidaEm: string | null;
};
export function isNotification(value: unknown): value is Notificacao {
  if (!value || typeof value !== 'object') return false;
  const n = value as Notificacao;
  return (
    Number.isInteger(n.idNotificacao) &&
    n.idNotificacao > 0 &&
    Number.isInteger(n.idAgendamento) &&
    n.idAgendamento > 0 &&
    typeof n.tipo === 'string' &&
    typeof n.mensagem === 'string' &&
    typeof n.criadaEm === 'string' &&
    !Number.isNaN(Date.parse(n.criadaEm)) &&
    (n.lidaEm === null || typeof n.lidaEm === 'string')
  );
}
export async function listNotifications(token: string, pagina = 0): Promise<Page<Notificacao>> {
  const p = await getJson<Page<Notificacao>>(
    `/prestadores/me/notificacoes?pagina=${pagina}&tamanho=5`,
    token,
  );
  if (
    !p ||
    !Array.isArray(p.itens) ||
    !p.itens.every(isNotification) ||
    !Number.isInteger(p.pagina) ||
    p.pagina < 0 ||
    !Number.isInteger(p.tamanho) ||
    p.tamanho < 1 ||
    !Number.isInteger(p.totalItens) ||
    p.totalItens < 0 ||
    !Number.isInteger(p.totalPaginas) ||
    p.totalPaginas < 0
  )
    throw new Error('Não foi possível carregar as notificações.');
  return p;
}
export async function readNotification(token: string, id: number): Promise<void> {
  await postJson(`/prestadores/me/notificacoes/${id}/lida`, {}, token);
}
export function connectNotifications(
  token: string,
  onNotification: (n: Notificacao) => void,
  callbacks: RealtimeCallbacks,
) {
  return connectRealtime(
    token,
    (client) => {
      client.subscribe('/user/queue/notificacoes', (event) => {
        try {
          const n: unknown = JSON.parse(event.body);
          if (isNotification(n)) onNotification(n);
        } catch {
          callbacks.onError();
        }
      });
    },
    callbacks,
  );
}
