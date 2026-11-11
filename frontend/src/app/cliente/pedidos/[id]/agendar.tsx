import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AlertBanner } from '@/components/alert-banner';
import { AppIcon } from '@/components/app-icon';
import { InfoBanner } from '@/components/info-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { SubmitButton } from '@/components/submit-button';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { listAddresses, type Endereco } from '@/services/addresses';
import { ApiError } from '@/services/api';
import {
  createAppointment,
  listQuoteSlots,
  type Disponibilidade,
  type Page,
} from '@/services/appointments';
import { getClientQuote, type Orcamento } from '@/services/quotes';
import { dateLabel } from '@/utils/appointment-format';
import { formatMoney, validRequestId } from '@/utils/quote-validation';

export default function BookingScreen() {
  const { id, orcamento } = useLocalSearchParams<{ id: string; orcamento: string }>();
  return <BookingForm key={`${id}:${orcamento}`} id={id} quoteId={orcamento} />;
}
function BookingForm({ id, quoteId }: { id: string; quoteId: string }) {
  const { colors } = useAppTheme(),
    { session, expireSession } = useSession();
  const token = session?.accessToken;
  const parsedQuote = validRequestId(quoteId),
    parsedOrder = validRequestId(id);
  const [quote, setQuote] = useState<Orcamento | null>(null),
    [addresses, setAddresses] = useState<Endereco[]>([]),
    [slots, setSlots] = useState<Page<Disponibilidade> | null>(null);
  const [page, setPage] = useState(0),
    [day, setDay] = useState(''),
    [slot, setSlot] = useState<number | null>(null),
    [address, setAddress] = useState<number | null>(null);
  const [loading, setLoading] = useState(true),
    [loadError, setLoadError] = useState<string | null>(null),
    [message, setMessage] = useState<string | null>(null),
    [errors, setErrors] = useState<Record<string, string>>({}),
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
    try {
      if (parsedQuote === null || parsedOrder === null)
        throw new Error('Orçamento não encontrado.');
      const [q, a, s] = await Promise.all([
        getClientQuote(token, parsedQuote),
        listAddresses(token),
        listQuoteSlots(token, parsedQuote, page),
      ]);
      if (q.idPedido !== parsedOrder) throw new Error('O orçamento não pertence a este pedido.');
      if (request === generation.current) {
        if (page > 0 && page >= s.totalPaginas) {
          setPage(Math.max(0, s.totalPaginas - 1));
          return;
        }
        setQuote(q);
        setAddresses(a);
        setSlots(s);
        setDay((current) =>
          s.itens.some((d) => d.data === current) ? current : (s.itens[0]?.data ?? ''),
        );
        setSlot((current) =>
          s.itens.some((d) => d.idDisponibilidade === current) ? current : null,
        );
        setAddress((current) => (a.some((e) => e.idEndereco === current) ? current : null));
      }
    } catch (failure) {
      if (request !== generation.current || (await expired(failure))) return;
      setLoadError(
        failure instanceof ApiError && failure.status === 404
          ? 'Este orçamento não está disponível na sua conta.'
          : failure instanceof Error
            ? failure.message
            : 'Não foi possível carregar os horários.',
      );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expired, page, parsedOrder, parsedQuote, token]);
  useFocusEffect(
    useCallback(() => {
      void load();
      return () => {
        generation.current++;
      };
    }, [load]),
  );
  async function confirm() {
    if (!token || !parsedQuote || submitting.current) return;
    const validation: Record<string, string> = {};
    if (!slot) validation.idDisponibilidade = 'Selecione um horário disponível';
    if (!address) validation.idEndereco = 'Confirme o endereço do serviço';
    setErrors(validation);
    setMessage(null);
    if (Object.keys(validation).length) {
      setMessage('Confira o horário e o endereço para continuar.');
      return;
    }
    const request = generation.current;
    submitting.current = true;
    setSaving(true);
    try {
      const result = await createAppointment(token, parsedQuote, slot!, address!);
      if (request === generation.current)
        router.replace(`/cliente/agendamentos/${result.idAgendamento}?novo=1`);
    } catch (failure) {
      if (request !== generation.current || (await expired(failure))) return;
      if (failure instanceof ApiError) {
        setErrors(failure.fields);
        if (failure.status === 409) await load();
      }
      setMessage(
        failure instanceof Error ? failure.message : 'Não foi possível confirmar o agendamento.',
      );
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  }
  const days = [...new Set(slots?.itens.map((d) => d.data) ?? [])];
  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title="Agendar serviço"
        onBack={() => {
          if (!submitting.current) router.dismissTo(`/cliente/pedidos/${id}/orcamentos`);
        }}
      />
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {loading ? (
          <View style={styles.loading}>
            <ActivityIndicator accessibilityLabel="Carregando horários disponíveis" />
          </View>
        ) : loadError ? (
          <>
            <ScreenState
              kind="error"
              title="Agendamento indisponível"
              message={loadError}
              actionLabel="Tentar novamente"
              onAction={load}
            />
            <Button onPress={() => router.navigate('/cliente/agendamentos')}>
              Ver meus agendamentos
            </Button>
          </>
        ) : (
          quote && (
            <>
              <View style={styles.provider}>
                <View style={[styles.avatar, { backgroundColor: colors.primaryContainer }]}>
                  <AppIcon name="user" color={colors.onPrimaryContainer} />
                </View>
                <View style={styles.text}>
                  <Text variant="titleMedium">{quote.prestador.nome}</Text>
                  <Text variant="bodySmall">Orçamento: {formatMoney(quote.valor)}</Text>
                  <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                    {quote.tituloPedido}
                  </Text>
                </View>
              </View>
              {!addresses.length ? (
                <ScreenState
                  kind="empty"
                  title="Cadastre o endereço do serviço"
                  message="Escolha um endereço da sua conta para confirmar o atendimento."
                  actionLabel="Cadastrar endereço"
                  onAction={() => router.push('/cliente/enderecos/novo')}
                />
              ) : !slots?.totalItens ? (
                <ScreenState
                  kind="empty"
                  title="Sem horários disponíveis"
                  message="Este prestador ainda não tem horários livres para agendamento."
                  actionLabel="Atualizar horários"
                  onAction={load}
                />
              ) : (
                <>
                  <Text variant="labelLarge">Escolha a data</Text>
                  <View
                    style={styles.options}
                    accessibilityRole="radiogroup"
                    accessibilityLabel="Datas disponíveis"
                  >
                    {days.map((d) => (
                      <Pressable
                        key={d}
                        accessibilityRole="radio"
                        accessibilityLabel={`Data ${dateLabel(d)}`}
                        accessibilityState={{ checked: d === day, disabled: saving }}
                        aria-checked={d === day}
                        disabled={saving}
                        onPress={() => {
                          setDay(d);
                          setSlot(null);
                        }}
                        style={[
                          styles.option,
                          {
                            borderColor: d === day ? colors.primaryContainer : colors.outline,
                            backgroundColor:
                              d === day ? colors.primaryContainer : colors.surfaceContainerLowest,
                          },
                        ]}
                      >
                        <Text variant="labelLarge">{dateLabel(d, false)}</Text>
                      </Pressable>
                    ))}
                  </View>
                  <Text variant="labelLarge">Escolha o horário</Text>
                  <View
                    style={styles.options}
                    accessibilityRole="radiogroup"
                    accessibilityLabel="Horários disponíveis"
                  >
                    {slots.itens
                      .filter((d) => d.data === day)
                      .map((d) => (
                        <Pressable
                          key={d.idDisponibilidade}
                          accessibilityRole="radio"
                          accessibilityLabel={`Horário ${d.hora.slice(0, 5)}`}
                          accessibilityState={{
                            checked: d.idDisponibilidade === slot,
                            disabled: saving,
                          }}
                          aria-checked={d.idDisponibilidade === slot}
                          disabled={saving}
                          onPress={() => {
                            setSlot(d.idDisponibilidade);
                            setErrors((current) => ({ ...current, idDisponibilidade: '' }));
                          }}
                          style={[
                            styles.option,
                            {
                              borderColor:
                                d.idDisponibilidade === slot
                                  ? colors.primaryContainer
                                  : colors.outline,
                              backgroundColor:
                                d.idDisponibilidade === slot
                                  ? colors.primaryContainer
                                  : colors.surfaceContainerLowest,
                            },
                          ]}
                        >
                          <Text variant="labelLarge">{d.hora.slice(0, 5)}</Text>
                        </Pressable>
                      ))}
                  </View>
                  {errors.idDisponibilidade && (
                    <Text style={{ color: colors.error }}>{errors.idDisponibilidade}</Text>
                  )}
                  {slots.totalPaginas > 1 && (
                    <View style={styles.pages}>
                      <Button disabled={saving || !page} onPress={() => setPage(page - 1)}>
                        Anterior
                      </Button>
                      <Text variant="bodySmall">
                        {page + 1} de {slots.totalPaginas}
                      </Text>
                      <Button
                        disabled={saving || page + 1 >= slots.totalPaginas}
                        onPress={() => setPage(page + 1)}
                      >
                        Próximos horários
                      </Button>
                    </View>
                  )}
                  <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                    Horário de Brasília
                  </Text>
                  <Text variant="labelLarge">Local de atendimento</Text>
                  <View
                    accessibilityRole="radiogroup"
                    accessibilityLabel="Endereço do serviço"
                    style={styles.addresses}
                  >
                    {addresses.map((e) => (
                      <Pressable
                        key={e.idEndereco}
                        accessibilityRole="radio"
                        accessibilityLabel={`${e.logradouro}, ${e.numero}`}
                        accessibilityState={{ checked: address === e.idEndereco, disabled: saving }}
                        aria-checked={address === e.idEndereco}
                        disabled={saving}
                        onPress={() => {
                          setAddress(e.idEndereco);
                          setErrors((current) => ({ ...current, idEndereco: '' }));
                        }}
                        style={[
                          styles.address,
                          {
                            borderColor:
                              address === e.idEndereco ? colors.primary : colors.outlineVariant,
                            backgroundColor:
                              address === e.idEndereco
                                ? colors.primaryContainer
                                : colors.surfaceVariant,
                          },
                        ]}
                      >
                        <Text variant="bodyLarge">
                          {e.logradouro}, {e.numero}
                        </Text>
                        {e.complemento && <Text variant="bodyMedium">{e.complemento}</Text>}
                        <Text variant="bodyMedium">
                          {e.bairro} · {e.cidade} / {e.uf}
                        </Text>
                      </Pressable>
                    ))}
                  </View>
                  {errors.idEndereco && (
                    <Text style={{ color: colors.error }}>{errors.idEndereco}</Text>
                  )}
                  <InfoBanner icon="info">
                    Ao confirmar, o horário fica reservado e o endereço combinado é compartilhado
                    com o prestador.
                  </InfoBanner>
                  {message && <AlertBanner>{message}</AlertBanner>}
                  <SubmitButton loading={saving} disabled={saving} onPress={confirm}>
                    {saving ? 'Agendando...' : 'Confirmar agendamento'}
                  </SubmitButton>
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
  content: { padding: 16, gap: 16, paddingBottom: 32 },
  loading: { alignItems: 'center', padding: 48 },
  provider: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 8 },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: { flex: 1, gap: 4 },
  options: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  option: {
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderWidth: 1,
    borderRadius: 8,
    minHeight: 40,
  },
  addresses: { gap: 8 },
  address: { borderWidth: 1, borderRadius: 8, padding: 16, gap: 4 },
  pages: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
});
