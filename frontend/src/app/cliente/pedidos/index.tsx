import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RequestListSkeleton } from '@/components/request-list-skeleton';
import { ScreenState } from '@/components/screen-state';
import { ServiceRequestCard } from '@/components/service-request-card';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { listOrders, type PaginaPedidos } from '@/services/orders';

export default function OrdersScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const [data, setData] = useState<PaginaPedidos | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const token = session?.accessToken;

  const load = useCallback(
    async (page = 0) => {
      if (!token) return;
      const request = ++generation.current;
      setLoading(true);
      setError(null);
      try {
        const response = await listOrders(token, page);
        if (request === generation.current) setData(response);
      } catch (failure) {
        if (request !== generation.current) return;
        if (failure instanceof ApiError && failure.status === 401) {
          await expireSession(token).catch(() => {
            console.error('Não foi possível limpar a sessão expirada.');
          });
        } else
          setError(
            failure instanceof Error ? failure.message : 'Não foi possível carregar seus pedidos.',
          );
      } finally {
        if (request === generation.current) setLoading(false);
      }
    },
    [expireSession, token],
  );

  useFocusEffect(
    useCallback(() => {
      void load();
      return () => {
        generation.current++;
      };
    }, [load]),
  );

  return (
    <SafeAreaView
      edges={['top', 'left', 'right']}
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title="Meus pedidos"
        onBack={() => router.dismissTo('/cliente')}
      />
      <ScrollView contentContainerStyle={styles.content}>
        <Button mode="contained" onPress={() => router.push('/cliente/pedidos/novo')}>
          Novo pedido
        </Button>
        {loading ? (
          <RequestListSkeleton label="Carregando pedidos" />
        ) : error ? (
          <ScreenState
            kind="error"
            title="Não foi possível carregar seus pedidos"
            message={error}
            actionLabel="Tentar novamente"
            onAction={() => {
              void load();
            }}
          />
        ) : !data || data.itens.length === 0 ? (
          <ScreenState
            kind="empty"
            title="Você ainda não tem pedidos"
            message="Conte o que precisa e publique seu primeiro pedido de serviço."
          />
        ) : (
          <>
            {data.itens.map((pedido) => (
              <ServiceRequestCard
                key={pedido.idPedido}
                pedido={pedido}
                showStatus
                accessibilityLabel={'Ver pedido ' + pedido.titulo}
                onPress={() =>
                  router.push({
                    pathname: '/cliente/pedidos/[id]',
                    params: { id: pedido.idPedido.toString() },
                  })
                }
              />
            ))}
            {data.totalPaginas > 1 && (
              <View style={styles.pagination}>
                <Button
                  disabled={data.pagina === 0}
                  onPress={() => {
                    void load(data.pagina - 1);
                  }}>
                  Anterior
                </Button>
                <Text variant="bodySmall">
                  {data.pagina + 1} de {data.totalPaginas}
                </Text>
                <Button
                  disabled={data.pagina + 1 >= data.totalPaginas}
                  onPress={() => {
                    void load(data.pagina + 1);
                  }}>
                  Próxima
                </Button>
              </View>
            )}
            <Button
              mode="text"
              onPress={() => {
                void load(data.pagina);
              }}>
              Atualizar pedidos
            </Button>
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    padding: 16,
    paddingTop: 8,
    paddingBottom: 24,
    gap: 16,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  pagination: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
