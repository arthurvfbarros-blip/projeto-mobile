import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { AlertBanner } from '@/components/alert-banner';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { type Page } from '@/services/appointments';
import {
  connectNotifications,
  listNotifications,
  readNotification,
  type Notificacao,
} from '@/services/notifications';

export function ProviderNotifications({ onNotification }: { onNotification?: () => void }) {
  const { colors } = useAppTheme(),
    { session, expireSession } = useSession();
  const token = session?.accessToken;
  const [page, setPage] = useState(0),
    [data, setData] = useState<Page<Notificacao> | null>(null),
    [error, setError] = useState<string | null>(null),
    [opening, setOpening] = useState<number | null>(null);
  const generation = useRef(0),
    busy = useRef(false);
  const failure = useCallback(
    async (error: unknown) => {
      if (error instanceof ApiError && error.status === 401 && token)
        await expireSession(token).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
      else
        setError(
          error instanceof Error ? error.message : 'Não foi possível carregar as notificações.',
        );
    },
    [expireSession, token],
  );
  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    try {
      const result = await listNotifications(token, page);
      if (request === generation.current) {
        if (page > 0 && page >= result.totalPaginas) {
          setPage(Math.max(0, result.totalPaginas - 1));
          return;
        }
        setData(result);
        setError(null);
      }
    } catch (error) {
      if (request === generation.current) await failure(error);
    }
  }, [failure, page, token]);
  useFocusEffect(
    useCallback(() => {
      if (!token) return;
      let active = true;
      void load();
      const socket = connectNotifications(
        token,
        () => {
          if (active) {
            void load();
            onNotification?.();
          }
        },
        {
          onConnected: () => {
            if (active) void load();
          },
          onDisconnected: () => {},
          onError: () => {},
        },
      );
      const interval = setInterval(() => {
        if (active) void load();
      }, 30000);
      return () => {
        active = false;
        generation.current++;
        clearInterval(interval);
        void socket.deactivate();
      };
    }, [load, onNotification, token]),
  );
  async function open(n: Notificacao) {
    if (!token || busy.current) return;
    busy.current = true;
    setOpening(n.idNotificacao);
    try {
      await readNotification(token, n.idNotificacao);
      router.push(`/prestador/agendamentos/${n.idAgendamento}`);
    } catch (error) {
      await failure(error);
    } finally {
      busy.current = false;
      setOpening(null);
    }
  }
  if (error)
    return (
      <View style={styles.error}>
        <AlertBanner>{error}</AlertBanner>
        <Button onPress={load}>Atualizar notificações</Button>
      </View>
    );
  if (!data?.totalItens) return null;
  return (
    <View style={[styles.container, { backgroundColor: colors.primaryContainer }]}>
      <Text variant="titleMedium">
        {data.totalItens} {data.totalItens === 1 ? 'nova notificação' : 'novas notificações'}
      </Text>
      {data.itens.map((n) => (
        <View key={n.idNotificacao} style={styles.item}>
          <Text variant="bodyMedium">{n.mensagem}</Text>
          <Button
            mode="outlined"
            loading={opening === n.idNotificacao}
            disabled={opening !== null}
            accessibilityLabel={`Ver contratação do agendamento ${n.idAgendamento}`}
            onPress={() => {
              void open(n);
            }}
          >
            Ver contratação
          </Button>
        </View>
      ))}
      {data.totalPaginas > 1 && (
        <View style={styles.pagination}>
          <Button disabled={page === 0} onPress={() => setPage(page - 1)}>
            Anterior
          </Button>
          <Text variant="bodySmall">
            {page + 1} de {data.totalPaginas}
          </Text>
          <Button disabled={page + 1 >= data.totalPaginas} onPress={() => setPage(page + 1)}>
            Próxima
          </Button>
        </View>
      )}
    </View>
  );
}
const styles = StyleSheet.create({
  container: { borderRadius: 12, padding: 16, gap: 16 },
  item: { gap: 8 },
  pagination: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  error: { gap: 8 },
});
