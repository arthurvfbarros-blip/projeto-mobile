import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { RequestListSkeleton } from '@/components/request-list-skeleton';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { listProviderSlots, type Disponibilidade, type Page } from '@/services/appointments';
import { dateLabel } from '@/utils/appointment-format';

export default function ProviderAvailability() {
  const { colors } = useAppTheme(),
    { session, expireSession } = useSession();
  const token = session?.accessToken;
  const [page, setPage] = useState(0),
    [data, setData] = useState<Page<Disponibilidade> | null>(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setError(null);
    setData(null);
    try {
      const result = await listProviderSlots(token, page);
      if (request === generation.current) {
        if (page > 0 && page >= result.totalPaginas) {
          setPage(Math.max(0, result.totalPaginas - 1));
          return;
        }
        setData(result);
      }
    } catch (failure) {
      if (request !== generation.current) return;
      if (failure instanceof ApiError && failure.status === 401)
        await expireSession(token).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
      else
        setError(
          failure instanceof Error ? failure.message : 'Não foi possível carregar os horários.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, page, token]);
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
        title="Minha disponibilidade"
        onBack={() => router.dismissTo('/prestador/agendamentos')}
      />
      <ScrollView contentContainerStyle={styles.content}>
        <Text variant="bodyMedium">
          Cadastre os horários em que pode iniciar um serviço. Horário de Brasília.
        </Text>
        <Button mode="contained" onPress={() => router.push('/prestador/disponibilidades/novo')}>
          Adicionar horário
        </Button>
        {loading ? (
          <RequestListSkeleton label="Carregando horários" />
        ) : error ? (
          <ScreenState
            kind="error"
            title="Não foi possível carregar os horários"
            message={error}
            actionLabel="Tentar novamente"
            onAction={load}
          />
        ) : data && !data.totalItens ? (
          <ScreenState
            kind="empty"
            title="Nenhum horário futuro"
            message="Adicione sua disponibilidade para os clientes poderem agendar."
          />
        ) : (
          data && (
            <>
              {data.itens.map((d) => (
                <View
                  key={d.idDisponibilidade}
                  style={[styles.card, { borderColor: colors.outlineVariant }]}
                >
                  <Text variant="titleMedium">
                    {dateLabel(d.data)} · {d.hora.slice(0, 5)}
                  </Text>
                  <Text variant="labelLarge" style={{ color: colors.primary }}>
                    {d.status === 'livre' ? 'Livre' : 'Ocupado'}
                  </Text>
                </View>
              ))}
              {data.totalPaginas > 1 && (
                <View style={styles.pages}>
                  <Button disabled={!page} onPress={() => setPage(page - 1)}>
                    Anterior
                  </Button>
                  <Text>
                    {page + 1} de {data.totalPaginas}
                  </Text>
                  <Button
                    disabled={page + 1 >= data.totalPaginas}
                    onPress={() => setPage(page + 1)}
                  >
                    Próxima
                  </Button>
                </View>
              )}
              <Button onPress={load}>Atualizar horários</Button>
            </>
          )
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, gap: 16, paddingBottom: 32 },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, gap: 8 },
  pages: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
});
