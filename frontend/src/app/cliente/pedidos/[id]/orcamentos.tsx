import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState, type ComponentRef } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppIcon } from '@/components/app-icon';
import { RequestListSkeleton } from '@/components/request-list-skeleton';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { getOrder, type Pedido } from '@/services/orders';
import { listQuotes, type PaginaOrcamentos } from '@/services/quotes';
import { formatCreatedAt, orderStatusLabel } from '@/utils/order-validation';
import {
  formatDeadline,
  formatMoney,
  quoteStatusLabel,
  validRequestId,
} from '@/utils/quote-validation';

export default function ReceivedQuotesScreen() {
  const { colors } = useAppTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const idPedido = validRequestId(id);
  const { session, expireSession } = useSession();
  const token = session?.accessToken;
  const [pedido, setPedido] = useState<Pedido | null>(null);
  const [data, setData] = useState<PaginaOrcamentos | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const scroll = useRef<ComponentRef<typeof ScrollView>>(null);

  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setError(null);
    setData(null);
    try {
      if (idPedido === null) throw new Error('Pedido não encontrado.');
      const [order, quotes] = await Promise.all([
        getOrder(token, idPedido),
        listQuotes(token, idPedido, page),
      ]);
      if (request === generation.current) {
        if (page > 0 && page >= quotes.totalPaginas) {
          setPage(Math.max(0, quotes.totalPaginas - 1));
          return;
        }
        setPedido(order);
        setData(quotes);
      }
    } catch (failure) {
      if (request !== generation.current) return;
      if (failure instanceof ApiError && failure.status === 401) {
        await expireSession(token).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
      } else
        setError(
          failure instanceof ApiError && failure.status === 404
            ? 'Este pedido não está disponível na sua conta.'
            : failure instanceof Error
              ? failure.message
              : 'Não foi possível carregar os orçamentos.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, idPedido, page, token]);
  useFocusEffect(
    useCallback(() => {
      void load();
      return () => {
        generation.current++;
      };
    }, [load]),
  );

  function changePage(next: number) {
    scroll.current?.scrollTo({ y: 0, animated: false });
    setPage(next);
  }
  const count = data?.totalItens ?? 0;
  return (
    <SafeAreaView
      edges={['top', 'left', 'right']}
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}
    >
      <ScreenHeader
        variant="app"
        title={pedido?.titulo ?? 'Orçamentos recebidos'}
        subtitle={
          pedido?.criadoEm ? `Pedido aberto em ${formatCreatedAt(pedido.criadoEm)}` : undefined
        }
        onBack={() => router.dismissTo(`/cliente/pedidos/${id}`)}
      />
      <ScrollView ref={scroll} contentContainerStyle={styles.content}>
        {loading ? (
          <RequestListSkeleton label="Carregando orçamentos" />
        ) : error ? (
          <ScreenState
            kind="error"
            title="Não foi possível carregar os orçamentos"
            message={error}
            actionLabel="Tentar novamente"
            onAction={load}
          />
        ) : (
          data && (
            <>
              <View style={styles.heading}>
                <Text variant="titleMedium">
                  {count} {count === 1 ? 'orçamento recebido' : 'orçamentos recebidos'}
                </Text>
                {!!count && (
                  <Text
                    variant="labelLarge"
                    style={[
                      styles.status,
                      {
                        backgroundColor: colors.secondaryContainer,
                        color: colors.onSecondaryContainer,
                      },
                    ]}
                  >
                    {pedido && ['aberto', 'em_orcamento'].includes(pedido.status)
                      ? 'Aguardando escolha'
                      : orderStatusLabel(pedido?.status ?? '')}
                  </Text>
                )}
              </View>
              {!count ? (
                <ScreenState
                  kind="empty"
                  title="Nenhum orçamento ainda"
                  message="Os orçamentos enviados pelos prestadores aparecerão aqui."
                  actionLabel="Atualizar orçamentos"
                  onAction={load}
                />
              ) : (
                <>
                  {data.itens.map((quote) => (
                    <View
                      key={quote.idOrcamento}
                      style={[
                        styles.card,
                        {
                          backgroundColor: colors.surface,
                          borderColor: colors.outlineVariant,
                        },
                      ]}
                    >
                      <View style={styles.provider}>
                        <View style={[styles.avatar, { backgroundColor: colors.primaryContainer }]}>
                          <AppIcon name="user" size={28} color={colors.onPrimaryContainer} />
                        </View>
                        <View style={styles.providerName}>
                          <Text variant="titleMedium">{quote.prestador.nome}</Text>
                          <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                            {quoteStatusLabel(quote.status)}
                          </Text>
                        </View>
                      </View>
                      <View style={styles.price}>
                        <Text variant="headlineSmall">{formatMoney(quote.valor)}</Text>
                        <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                          {formatDeadline(quote.prazoDias)}
                        </Text>
                      </View>
                      {quote.descricao && (
                        <Text variant="bodyMedium" selectable>
                          {quote.descricao}
                        </Text>
                      )}
                      {quote.status === 'enviado' &&
                        pedido &&
                        ['aberto', 'em_orcamento'].includes(pedido.status) && (
                          <Button
                            mode="contained"
                            accessibilityLabel={`Agendar com ${quote.prestador.nome}`}
                            onPress={() =>
                              router.push(
                                `/cliente/pedidos/${id}/agendar?orcamento=${quote.idOrcamento}`,
                              )
                            }
                          >
                            Agendar
                          </Button>
                        )}
                    </View>
                  ))}
                  {data.totalPaginas > 1 && (
                    <View style={styles.pagination}>
                      <Button disabled={page === 0} onPress={() => changePage(page - 1)}>
                        Anterior
                      </Button>
                      <Text variant="bodySmall">
                        {page + 1} de {data.totalPaginas}
                      </Text>
                      <Button
                        disabled={page + 1 >= data.totalPaginas}
                        onPress={() => changePage(page + 1)}
                      >
                        Próxima
                      </Button>
                    </View>
                  )}
                  <Button
                    mode="outlined"
                    icon={({ color }) => <AppIcon name="refresh" color={color} size={18} />}
                    onPress={load}
                  >
                    Atualizar orçamentos
                  </Button>
                </>
              )}
            </>
          )
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingTop: 8, paddingBottom: 32, gap: 12 },
  heading: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
  },
  status: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 8 },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, gap: 12 },
  provider: { flexDirection: 'row', gap: 12, alignItems: 'center' },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  providerName: { flex: 1, gap: 4 },
  price: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
  },
  pagination: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
});
