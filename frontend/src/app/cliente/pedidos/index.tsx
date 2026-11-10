import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { listOrders, type PaginaPedidos } from '@/services/orders';
import { formatCreatedAt, formatOrderDate, orderStatusLabel } from '@/utils/order-validation';

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
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader title="Meus pedidos" onBack={() => router.dismissTo('/cliente')} />
      <ScrollView contentContainerStyle={styles.content}>
        <Button
          mode="contained"
          contentStyle={styles.button}
          onPress={() => router.push('/cliente/pedidos/novo')}>
          Novo pedido
        </Button>
        <View style={styles.presentation}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Seus pedidos de serviço
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Acompanhe as solicitações que você publicou.
          </Text>
        </View>
        {loading ? (
          <View style={styles.state}>
            <ActivityIndicator accessibilityLabel="Carregando pedidos" />
            <Text>Carregando seus pedidos...</Text>
          </View>
        ) : error ? (
          <View style={styles.actions}>
            <AlertBanner>{error}</AlertBanner>
            <Button
              mode="outlined"
              onPress={() => {
                void load();
              }}>
              Tentar novamente
            </Button>
          </View>
        ) : !data || data.itens.length === 0 ? (
          <View style={[styles.empty, { borderColor: colors.outlineVariant }]}>
            <Text variant="titleMedium" accessibilityRole="header">
              Você ainda não tem pedidos
            </Text>
            <Text variant="bodyMedium">
              Conte o que precisa e publique seu primeiro pedido de serviço.
            </Text>
          </View>
        ) : (
          <View style={styles.cards}>
            {data.itens.map((pedido) => (
              <View
                key={pedido.idPedido}
                style={[styles.card, { borderColor: colors.outlineVariant }]}>
                <Text variant="titleMedium">{pedido.titulo}</Text>
                <Text variant="labelLarge" style={{ color: colors.primary }}>
                  {orderStatusLabel(pedido.status)}
                </Text>
                <Text variant="bodyMedium">{pedido.categoria.nome}</Text>
                {pedido.cidade ? (
                  <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                    {pedido.bairro ? `${pedido.bairro} · ` : ''}
                    {pedido.cidade} / {pedido.uf}
                  </Text>
                ) : null}
                <Text variant="bodySmall">
                  Data desejada: {formatOrderDate(pedido.dataDesejada)}
                </Text>
                <Text variant="bodySmall">Publicado em: {formatCreatedAt(pedido.criadoEm)}</Text>
                <Button
                  mode="text"
                  accessibilityLabel={`Ver pedido ${pedido.titulo}`}
                  onPress={() =>
                    router.push({
                      pathname: '/cliente/pedidos/[id]',
                      params: { id: pedido.idPedido.toString() },
                    })
                  }>
                  Ver pedido
                </Button>
              </View>
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
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    padding: 20,
    paddingBottom: 32,
    gap: 24,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  presentation: { gap: 8 },
  button: { height: 52 },
  state: { paddingVertical: 32, alignItems: 'center', gap: 16 },
  empty: { gap: 12, padding: 20, borderWidth: 1, borderRadius: 12 },
  cards: { gap: 16 },
  card: { gap: 8, padding: 16, borderWidth: 1, borderRadius: 12 },
  actions: { gap: 12 },
  pagination: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
