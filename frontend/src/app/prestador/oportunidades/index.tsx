import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppIcon } from '@/components/app-icon';
import { RequestListSkeleton } from '@/components/request-list-skeleton';
import { ScreenState } from '@/components/screen-state';
import { ServiceRequestCard } from '@/components/service-request-card';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { listOpportunities, opportunityError } from '@/services/opportunities';
import { type PaginaPedidos } from '@/services/orders';

export default function OpportunitiesScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const [data, setData] = useState<PaginaPedidos | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [accessDenied, setAccessDenied] = useState(false);
  const page = useRef(0);
  const generation = useRef(0);
  const token = session?.accessToken;

  const load = useCallback(
    async (requestedPage = page.current) => {
      if (!token) return;
      const request = ++generation.current;
      page.current = requestedPage;
      setLoading(true);
      setError(null);
      setAccessDenied(false);
      setData(null);
      try {
        let response = await listOpportunities(token, requestedPage);
        if (request !== generation.current) return;
        if (
          response.totalItens > 0 &&
          response.itens.length === 0 &&
          requestedPage >= response.totalPaginas
        ) {
          response = await listOpportunities(token, response.totalPaginas - 1);
        }
        if (request !== generation.current) return;
        page.current = response.pagina;
        setData(response);
      } catch (failure) {
        if (request !== generation.current) return;
        if (failure instanceof ApiError && failure.status === 401) {
          await expireSession(token).catch(() => {
            console.error('Não foi possível limpar a sessão expirada.');
          });
        } else {
          setAccessDenied(failure instanceof ApiError && failure.status === 403);
          setError(opportunityError(failure));
        }
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

  const subtitle =
    loading || error || !data
      ? undefined
      : data.totalItens === 0
        ? 'Nenhum pedido compatível agora'
        : data.totalItens === 1
          ? '1 pedido compatível com seu perfil'
          : data.totalItens + ' pedidos compatíveis com seu perfil';

  return (
    <SafeAreaView
      edges={['top', 'left', 'right']}
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader variant="app" title="Oportunidades" subtitle={subtitle} />
      <ScrollView contentContainerStyle={styles.content}>
        {loading ? (
          <RequestListSkeleton label="Carregando oportunidades" />
        ) : error ? (
          <ScreenState
            kind={accessDenied ? 'restricted' : 'error'}
            title={
              accessDenied ? 'Confira seu cadastro' : 'Não foi possível carregar as oportunidades'
            }
            message={error}
            actionLabel={accessDenied ? 'Conferir cadastro' : 'Tentar novamente'}
            onAction={
              accessDenied
                ? () => router.dismissTo('/prestador')
                : () => {
                    void load();
                  }
            }
          />
        ) : data ? (
          data.itens.length === 0 ? (
            <ScreenState
              kind="empty"
              title="Nenhuma oportunidade por enquanto"
              message="Os pedidos compatíveis com seus serviços e região aparecerão aqui."
              actionLabel="Atualizar oportunidades"
              onAction={() => {
                void load();
              }}
            />
          ) : (
            <>
              {data.itens.map((pedido) => (
                <ServiceRequestCard
                  key={pedido.idPedido}
                  pedido={pedido}
                  accessibilityLabel={'Ver oportunidade ' + pedido.titulo}
                  onPress={() =>
                    router.push({
                      pathname: '/prestador/oportunidades/[id]',
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
                mode="outlined"
                icon={({ color }) => <AppIcon name="refresh" color={color} size={18} />}
                onPress={() => {
                  void load();
                }}>
                Atualizar oportunidades
              </Button>
            </>
          )
        ) : null}
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
    gap: 12,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  pagination: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
