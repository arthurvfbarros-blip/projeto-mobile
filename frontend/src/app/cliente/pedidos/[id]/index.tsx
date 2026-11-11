import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { OrderSummary } from '@/components/order-summary';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { getOrder, type Pedido } from '@/services/orders';
import { formatCreatedAt, orderStatusLabel } from '@/utils/order-validation';

export default function OrderDetailsScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const { id, pedidoCriado } = useLocalSearchParams<{
    id: string;
    pedidoCriado?: string;
  }>();
  const [pedido, setPedido] = useState<Pedido | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const generation = useRef(0);
  const token = session?.accessToken;

  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setError(null);
    setNotFound(false);
    setPedido(null);
    const idPedido = Number(id);
    if (
      !/^\d+$/.test(id ?? '') ||
      !Number.isInteger(idPedido) ||
      idPedido <= 0 ||
      idPedido > 2_147_483_647
    ) {
      setNotFound(true);
      setLoading(false);
      return;
    }
    try {
      const response = await getOrder(token, idPedido);
      if (request === generation.current) setPedido(response);
    } catch (failure) {
      if (request !== generation.current) return;
      if (failure instanceof ApiError && failure.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else if (failure instanceof ApiError && failure.status === 404) setNotFound(true);
      else
        setError(
          failure instanceof Error ? failure.message : 'Não foi possível carregar o pedido.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, id, token]);

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
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}
    >
      <ScreenHeader
        variant="app"
        title="Detalhes do pedido"
        onBack={() => router.dismissTo('/cliente/pedidos')}
      />
      <ScrollView contentContainerStyle={styles.content}>
        {loading ? (
          <View style={styles.state}>
            <ActivityIndicator accessibilityLabel="Carregando pedido" />
            <Text>Carregando pedido...</Text>
          </View>
        ) : notFound ? (
          <>
            <Text variant="headlineSmall" accessibilityRole="header">
              Pedido não encontrado
            </Text>
            <Text>Volte à lista para consultar seus pedidos.</Text>
          </>
        ) : error ? (
          <>
            <AlertBanner>{error}</AlertBanner>
            <Button
              mode="outlined"
              onPress={() => {
                void load();
              }}
            >
              Tentar novamente
            </Button>
          </>
        ) : pedido ? (
          <>
            <View style={styles.presentation}>
              <Text variant="headlineSmall" accessibilityRole="header">
                {pedido.titulo}
              </Text>
              <Text variant="labelLarge" style={{ color: colors.primary }}>
                {orderStatusLabel(pedido.status)}
              </Text>
              <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                Publicado em: {formatCreatedAt(pedido.criadoEm)}
              </Text>
            </View>
            <View style={[styles.card, { borderColor: colors.outlineVariant }]}>
              <OrderSummary
                showTitle={false}
                titulo={pedido.titulo}
                descricao={pedido.descricao}
                categoria={pedido.categoria.nome}
                dataDesejada={pedido.dataDesejada}
                endereco={pedido.endereco}
              />
            </View>
            <Button
              mode="contained"
              onPress={() => router.push(`/cliente/pedidos/${id}/orcamentos`)}
            >
              Ver orçamentos
            </Button>
            <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
              A data desejada é uma preferência. O agendamento será confirmado em outra etapa.
            </Text>
          </>
        ) : null}
        <Button mode="outlined" onPress={() => router.dismissTo('/cliente/pedidos')}>
          Meus pedidos
        </Button>
      </ScrollView>
      <Snackbar
        visible={pedidoCriado === '1'}
        onDismiss={() => router.setParams({ pedidoCriado: undefined })}
      >
        Pedido enviado com sucesso!
      </Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    padding: 16,
    paddingBottom: 32,
    gap: 16,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  state: { paddingVertical: 32, alignItems: 'center', gap: 16 },
  presentation: { gap: 8 },
  card: { padding: 16, borderWidth: 1, borderRadius: 12 },
});
