import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AppIcon } from '@/components/app-icon';
import { InfoBanner } from '@/components/info-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { getAppointment, type AgendaProfile, type Agendamento } from '@/services/appointments';
import { appointmentStatus, dateLabel } from '@/utils/appointment-format';
import { formatMoney, validRequestId } from '@/utils/quote-validation';

export function AppointmentDetails({ profile }: { profile: AgendaProfile }) {
  const { colors, fonts } = useAppTheme(),
    { session, expireSession } = useSession();
  const { id, novo, contratacaoConfirmada } = useLocalSearchParams<{
    id: string;
    novo?: string;
    contratacaoConfirmada?: string;
  }>();
  const token = session?.accessToken;
  const [data, setData] = useState<Agendamento | null>(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const list = profile === 'cliente' ? '/cliente/agendamentos' : '/prestador/agendamentos';
  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setError(null);
    setData(null);
    try {
      const parsed = validRequestId(id);
      if (parsed === null) throw new Error('Agendamento não encontrado.');
      const result = await getAppointment(token, profile, parsed);
      if (request === generation.current) setData(result);
    } catch (failure) {
      if (request !== generation.current) return;
      if (failure instanceof ApiError && failure.status === 401)
        await expireSession(token).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
      else
        setError(
          failure instanceof ApiError && failure.status === 404
            ? 'Este agendamento não está disponível na sua conta.'
            : failure instanceof Error
              ? failure.message
              : 'Não foi possível carregar o agendamento.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, id, profile, token]);
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
      <ScreenHeader variant="app" title="Agendamento" onBack={() => router.dismissTo(list)} />
      <ScrollView contentContainerStyle={styles.content}>
        {loading ? (
          <View style={styles.loading}>
            <ActivityIndicator accessibilityLabel="Carregando agendamento" />
          </View>
        ) : error ? (
          <ScreenState
            kind="error"
            title="Agendamento indisponível"
            message={error}
            actionLabel="Tentar novamente"
            onAction={load}
          />
        ) : (
          data && (
            <>
              {novo === '1' && data.status === 'confirmado' && (
                <View style={styles.hero}>
                  <View style={[styles.check, { backgroundColor: colors.successContainer }]}>
                    <AppIcon name="check" size={42} color={colors.success} />
                  </View>
                  <Text
                    variant="headlineSmall"
                    style={[styles.center, { fontFamily: fonts.titleMedium.fontFamily }]}
                    accessibilityRole="header"
                  >
                    Agendamento confirmado!
                  </Text>
                  <Text variant="bodyLarge" style={styles.center}>
                    Tudo certo para o seu serviço.
                  </Text>
                </View>
              )}
              <Text
                variant="labelLarge"
                style={[
                  styles.status,
                  {
                    color:
                      data.status === 'confirmado'
                        ? colors.onSuccessContainer
                        : colors.onPrimaryContainer,
                    backgroundColor:
                      data.status === 'confirmado'
                        ? colors.successContainer
                        : colors.primaryContainer,
                  },
                ]}
              >
                {data.contratacao && data.status === 'confirmado'
                  ? 'Serviço contratado'
                  : appointmentStatus(data.status)}
              </Text>
              <View
                style={[
                  styles.card,
                  { borderColor: colors.outlineVariant, backgroundColor: colors.surface },
                ]}
              >
                <Text variant="titleMedium">{data.tituloPedido}</Text>
                <Text variant="bodyMedium">
                  {profile === 'cliente' ? data.nomePrestador : data.nomeCliente}
                </Text>
                <View style={[styles.divider, { backgroundColor: colors.outlineVariant }]} />
                <Text variant="labelLarge">Data e horário</Text>
                <Text variant="bodyLarge">
                  {dateLabel(data.data)} · {data.hora.slice(0, 5)}
                </Text>
                <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                  Horário de Brasília
                </Text>
                <Text variant="labelLarge">Endereço do serviço</Text>
                <Text variant="bodyLarge" selectable>
                  {data.endereco.logradouro}, {data.endereco.numero}
                </Text>
                {data.endereco.complemento && (
                  <Text variant="bodyMedium">{data.endereco.complemento}</Text>
                )}
                <Text variant="bodyMedium">
                  {data.endereco.bairro} · {data.endereco.cidade} / {data.endereco.uf}
                </Text>
                <Text variant="bodySmall">CEP {data.endereco.cep}</Text>
                <Text variant="labelLarge">Valor do orçamento</Text>
                <Text variant="bodyLarge">{formatMoney(data.valor)}</Text>
                <Text variant="bodySmall">Agendamento #{data.idAgendamento}</Text>
              </View>
              {data.contratacao && (
                <View
                  style={[
                    styles.card,
                    {
                      borderColor: colors.outlineVariant,
                      backgroundColor: colors.successContainer,
                    },
                  ]}
                >
                  <Text variant="titleMedium">Contratação confirmada</Text>
                  <Text variant="bodyMedium">
                    Valor acordado: {formatMoney(data.contratacao.valor)}
                  </Text>
                  <Text variant="bodySmall">
                    Confirmado em{' '}
                    {new Intl.DateTimeFormat('pt-BR', {
                      dateStyle: 'short',
                      timeStyle: 'short',
                      timeZone: 'America/Sao_Paulo',
                    }).format(new Date(data.contratacao.confirmadaEm))}
                  </Text>
                </View>
              )}
              {profile === 'cliente' && data.status === 'confirmado' && !data.contratacao && (
                <Button
                  mode="contained"
                  onPress={() => router.push(`/cliente/agendamentos/${id}/contratacao`)}
                >
                  Confirmar contratação
                </Button>
              )}
              {data.status === 'confirmado' && (
                <InfoBanner icon="info">
                  Seu horário está reservado. Acompanhe este agendamento pela Agenda.
                </InfoBanner>
              )}
              {data.status === 'confirmado' && (
                <Button
                  mode="contained"
                  onPress={() =>
                    router.push(
                      profile === 'cliente'
                        ? `/cliente/agendamentos/${data.idAgendamento}/chat`
                        : `/prestador/agendamentos/${data.idAgendamento}/chat`,
                    )
                  }
                >
                  Mensagem
                </Button>
              )}
              <Button mode="outlined" onPress={() => router.dismissTo(list)}>
                Ver meus agendamentos
              </Button>
              {profile === 'cliente' && (
                <Button mode="outlined" onPress={() => router.dismissTo('/cliente')}>
                  Voltar para início
                </Button>
              )}
            </>
          )
        )}
      </ScrollView>
      <Snackbar
        visible={contratacaoConfirmada === '1'}
        onDismiss={() => router.setParams({ contratacaoConfirmada: undefined })}
      >
        Contratação confirmada com sucesso!
      </Snackbar>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 20, gap: 20, paddingBottom: 32 },
  loading: { alignItems: 'center', padding: 48 },
  hero: { alignItems: 'center', gap: 12, paddingVertical: 8 },
  check: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  center: { textAlign: 'center' },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, gap: 12 },
  divider: { height: 1, marginVertical: 4 },
  status: { alignSelf: 'center', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 16 },
});
