import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ProviderNotifications } from '@/components/provider-notifications';
import { AppIcon } from '@/components/app-icon';
import { RequestListSkeleton } from '@/components/request-list-skeleton';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import {
  listAppointments,
  type AgendaProfile,
  type AgendaPeriod,
  type Agendamento,
  type Page,
} from '@/services/appointments';
import { appointmentStatus, dateLabel } from '@/utils/appointment-format';

const periods = [
  ['proximos', 'Próximos'],
  ['historico', 'Histórico'],
  ['cancelados', 'Cancelados'],
] as const;
export function AppointmentList({ profile }: { profile: AgendaProfile }) {
  const { colors } = useAppTheme(),
    { session, expireSession } = useSession();
  const token = session?.accessToken;
  const [period, setPeriod] = useState<AgendaPeriod>('proximos'),
    [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Agendamento> | null>(null),
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
      const result = await listAppointments(token, profile, period, page);
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
          failure instanceof Error ? failure.message : 'Não foi possível carregar a agenda.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, page, period, profile, token]);
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
      <ScreenHeader variant="app" title="Meus agendamentos" />
      <ScrollView contentContainerStyle={styles.content}>
        {profile === 'prestador' && <ProviderNotifications onNotification={load} />}
        <View style={styles.tabs}>
          {periods.map(([value, label]) => (
            <Button
              key={value}
              mode={value === period ? 'contained-tonal' : 'outlined'}
              onPress={() => {
                setPeriod(value);
                setPage(0);
              }}
              accessibilityState={{ selected: value === period }}
              aria-selected={value === period}
            >
              {label}
            </Button>
          ))}
        </View>
        {profile === 'prestador' && (
          <Button mode="outlined" onPress={() => router.push('/prestador/disponibilidades')}>
            Minha disponibilidade
          </Button>
        )}
        {loading ? (
          <RequestListSkeleton label="Carregando agendamentos" />
        ) : error ? (
          <ScreenState
            kind="error"
            title="Não foi possível carregar a agenda"
            message={error}
            actionLabel="Tentar novamente"
            onAction={load}
          />
        ) : data && !data.totalItens ? (
          <ScreenState
            kind="empty"
            title="Nenhum agendamento por aqui"
            message={
              period === 'proximos'
                ? 'Seus próximos serviços agendados aparecerão aqui.'
                : 'Não há agendamentos neste período.'
            }
            actionLabel="Atualizar agenda"
            onAction={load}
          />
        ) : (
          data && (
            <>
              {data.itens.map((a) => (
                <View
                  key={a.idAgendamento}
                  style={[
                    styles.card,
                    { borderColor: colors.outlineVariant, backgroundColor: colors.surface },
                  ]}
                >
                  <View style={styles.between}>
                    <Text
                      variant="labelMedium"
                      style={[
                        styles.status,
                        {
                          backgroundColor:
                            a.status === 'cancelado'
                              ? colors.errorContainer
                              : colors.primaryContainer,
                          color:
                            a.status === 'cancelado'
                              ? colors.onErrorContainer
                              : colors.onPrimaryContainer,
                        },
                      ]}
                    >
                      {a.contratacao && a.status === 'confirmado'
                        ? 'Serviço contratado'
                        : a.status === 'confirmado' && a.situacaoConversa === 'aguardando_conversa'
                          ? 'Aguardando conversa'
                          : a.status === 'confirmado' && a.situacaoConversa === 'em_conversa'
                            ? 'Conversa iniciada'
                            : appointmentStatus(a.status)}
                    </Text>
                    <Text variant="bodySmall">#{a.idAgendamento}</Text>
                  </View>
                  <Text variant="titleMedium">{a.tituloPedido}</Text>
                  <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                    {profile === 'cliente' ? a.nomePrestador : a.nomeCliente}
                  </Text>
                  <View style={styles.row}>
                    <AppIcon name="calendar" size={18} />
                    <Text variant="bodyMedium">
                      {dateLabel(a.data)} · {a.hora.slice(0, 5)}
                    </Text>
                  </View>
                  <View style={styles.row}>
                    <AppIcon name="pin" size={18} />
                    <Text variant="bodyMedium" style={styles.text}>
                      {a.endereco.logradouro}, {a.endereco.numero}
                    </Text>
                  </View>
                  {a.status === 'confirmado' && (
                    <Button
                      mode="contained"
                      accessibilityLabel={`Mensagem sobre ${a.tituloPedido}`}
                      onPress={() =>
                        router.push(
                          profile === 'cliente'
                            ? `/cliente/agendamentos/${a.idAgendamento}/chat`
                            : `/prestador/agendamentos/${a.idAgendamento}/chat`,
                        )
                      }
                    >
                      Mensagem
                    </Button>
                  )}
                  <Button
                    mode="outlined"
                    onPress={() =>
                      router.push(
                        profile === 'cliente'
                          ? `/cliente/agendamentos/${a.idAgendamento}`
                          : `/prestador/agendamentos/${a.idAgendamento}`,
                      )
                    }
                  >
                    Detalhes
                  </Button>
                </View>
              ))}
              {data.totalPaginas > 1 && (
                <View style={styles.between}>
                  <Button disabled={page === 0} onPress={() => setPage(page - 1)}>
                    Anterior
                  </Button>
                  <Text variant="bodySmall">
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
              <Button onPress={load}>Atualizar agenda</Button>
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
  tabs: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, gap: 12 },
  status: { borderRadius: 16, paddingHorizontal: 12, paddingVertical: 6 },
  between: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  text: { flex: 1 },
});
