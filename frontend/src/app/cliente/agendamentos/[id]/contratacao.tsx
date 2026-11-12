import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AlertBanner } from '@/components/alert-banner';
import { InfoBanner } from '@/components/info-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { SubmitButton } from '@/components/submit-button';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { confirmContract, reviewContract, type RevisaoContratacao } from '@/services/contracts';
import { dateLabel } from '@/utils/appointment-format';
import { formatMoney, validRequestId } from '@/utils/quote-validation';

export default function ReviewContractScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <ReviewContract key={id} id={id} />;
}
function ReviewContract({ id }: { id: string }) {
  const { colors } = useAppTheme(),
    { session, expireSession } = useSession();
  const token = session?.accessToken;
  const appointment = validRequestId(id);
  const [data, setData] = useState<RevisaoContratacao | null>(null),
    [loading, setLoading] = useState(true),
    [loadError, setLoadError] = useState<string | null>(null),
    [error, setError] = useState<string | null>(null),
    [saving, setSaving] = useState(false);
  const generation = useRef(0),
    submitting = useRef(false);
  const expired = useCallback(
    async (failure: unknown) => {
      if (failure instanceof ApiError && failure.status === 401 && token) {
        await expireSession(token).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
        return true;
      }
      return false;
    },
    [expireSession, token],
  );
  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setLoadError(null);
    setData(null);
    try {
      if (appointment === null) throw new Error('Agendamento não encontrado.');
      const result = await reviewContract(token, appointment);
      if (request === generation.current) setData(result);
    } catch (failure) {
      if (request === generation.current && !(await expired(failure)))
        setLoadError(
          failure instanceof ApiError && failure.status === 404
            ? 'Este agendamento não está disponível na sua conta.'
            : failure instanceof Error
              ? failure.message
              : 'Não foi possível carregar a contratação.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [appointment, expired, token]);
  useFocusEffect(
    useCallback(() => {
      void load();
      return () => {
        generation.current++;
      };
    }, [load]),
  );
  async function confirm() {
    if (!token || appointment === null || !data?.podeConfirmar || submitting.current) return;
    const request = generation.current;
    submitting.current = true;
    setSaving(true);
    setError(null);
    try {
      await confirmContract(token, appointment);
      if (request === generation.current)
        router.replace(`/cliente/agendamentos/${id}?contratacaoConfirmada=1`);
    } catch (failure) {
      if (request === generation.current && !(await expired(failure))) {
        if (failure instanceof ApiError && failure.status === 409) await load();
        setError(
          failure instanceof Error ? failure.message : 'Não foi possível confirmar a contratação.',
        );
      }
    } finally {
      submitting.current = false;
      if (request === generation.current) setSaving(false);
    }
  }
  const a = data?.agendamento;
  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title="Confirmar contratação"
        onBack={() => {
          if (!submitting.current) router.dismissTo(`/cliente/agendamentos/${id}`);
        }}
      />
      <ScrollView contentContainerStyle={styles.content}>
        {loading ? (
          <View style={styles.loading}>
            <ActivityIndicator accessibilityLabel="Carregando revisão da contratação" />
          </View>
        ) : loadError ? (
          <ScreenState
            kind="error"
            title="Contratação indisponível"
            message={loadError}
            actionLabel="Tentar novamente"
            onAction={load}
          />
        ) : (
          a && (
            <>
              <Text variant="headlineSmall">Revise o acordo com o prestador</Text>
              <View
                style={[
                  styles.card,
                  { borderColor: colors.outlineVariant, backgroundColor: colors.surface },
                ]}
              >
                <Text variant="titleMedium">{a.tituloPedido}</Text>
                <Text variant="bodyLarge">{a.nomePrestador}</Text>
                <Text variant="labelLarge">Valor acordado</Text>
                <Text variant="headlineSmall">{formatMoney(a.contratacao?.valor ?? a.valor)}</Text>
                <Text variant="labelLarge">Data e horário</Text>
                <Text variant="bodyLarge">
                  {dateLabel(a.data)} · {a.hora.slice(0, 5)}
                </Text>
                <Text variant="bodySmall">Horário de Brasília</Text>
                <Text variant="labelLarge">Endereço do serviço</Text>
                <Text variant="bodyLarge">
                  {a.endereco.logradouro}, {a.endereco.numero}
                </Text>
                {a.endereco.complemento && (
                  <Text variant="bodyMedium">{a.endereco.complemento}</Text>
                )}
                <Text variant="bodyMedium">
                  {a.endereco.bairro} · {a.endereco.cidade} / {a.endereco.uf}
                </Text>
              </View>
              <InfoBanner icon="info">
                {data.podeConfirmar
                  ? 'Ao confirmar, você registra o acordo com o prestador para este serviço.'
                  : (data.motivoBloqueio ?? 'Revise o agendamento antes de continuar.')}
              </InfoBanner>
              {error && <AlertBanner>{error}</AlertBanner>}
              {!a.contratacao && (
                <SubmitButton
                  loading={saving}
                  disabled={saving || !data.podeConfirmar}
                  onPress={confirm}
                >
                  {saving ? 'Confirmando...' : 'Confirmar contratação'}
                </SubmitButton>
              )}
              {!data.podeConfirmar && !a.contratacao && a.status === 'confirmado' && (
                <Button
                  mode="outlined"
                  onPress={() => router.push(`/cliente/agendamentos/${id}/chat`)}
                >
                  Abrir conversa
                </Button>
              )}
              <Button disabled={saving} onPress={load}>
                Atualizar revisão
              </Button>
              <Button
                mode="outlined"
                disabled={saving}
                onPress={() => router.dismissTo(`/cliente/agendamentos/${id}`)}
              >
                Voltar ao agendamento
              </Button>
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
  card: { padding: 16, gap: 12, borderWidth: 1, borderRadius: 12 },
  loading: { alignItems: 'center', padding: 48 },
});
