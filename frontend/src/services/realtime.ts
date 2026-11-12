import 'fast-text-encoding';
import { Client } from '@stomp/stompjs';
import { API_URL } from '@/services/api';

export type RealtimeCallbacks = {
  onConnected: () => void;
  onDisconnected: () => void;
  onError: () => void;
};
export function connectRealtime(
  token: string,
  subscribe: (client: Client) => void,
  callbacks: RealtimeCallbacks,
): Client {
  const client = new Client({
    brokerURL: API_URL.replace(/^http/, 'ws') + '/ws-chat',
    connectHeaders: { Authorization: 'Bearer ' + token },
    reconnectDelay: 5000,
    connectionTimeout: 10000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      subscribe(client);
      callbacks.onConnected();
    },
    onWebSocketClose: callbacks.onDisconnected,
    onWebSocketError: callbacks.onError,
    onStompError: callbacks.onError,
  });
  client.activate();
  return client;
}
