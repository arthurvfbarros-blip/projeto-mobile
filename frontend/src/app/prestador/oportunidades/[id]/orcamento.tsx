import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { InfoBanner } from '@/components/info-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { SubmitButton } from '@/components/submit-button';
import { TextField } from '@/components/text-field';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { getOpportunity, opportunityError } from '@/services/opportunities';
import { createQuote, getMyQuote, type Orcamento } from '@/services/quotes';
import {
  formatDeadline,
  formatMoney,
  parseQuoteValue,
  quoteStatusLabel,
  validRequestId,
} from '@/utils/quote-validation';

export default function QuoteScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <QuoteForm key={id} id={id} />;
}

function QuoteForm({ id }: { id: string }) {
  const { colors } = useAppTheme();
  const idPedido = validRequestId(id);
  const { session, expireSession } = useSession();
  const token = session?.accessToken;
  const [title, setTitle] = useState('');
  const [quote, setQuote] = useState<Orcamento | null>(null);
  const [valor, setValor] = useState('');
  const [prazo, setPrazo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(false);
  const generation = useRef(0);
  const submitting = useRef(false);

  const handleExpired = useCallback(
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
      if (idPedido === null) throw new Error('Pedido não encontrado.');
      const existing = await getMyQuote(token, idPedido).catch((failure) => {
        if (failure instanceof ApiError && failure.status === 404) return null;
        throw failure;
      });
      if (existing) {
        if (request === generation.current) {
          setQuote(existing);
          setTitle(existing.tituloPedido);
        }
      } else {
        const opportunity = await getOpportunity(token, idPedido);
        if (request === generation.current) {
          setQuote(null);
          setTitle(opportunity.titulo);
        }
      }
    } catch (failure) {
      if (request === generation.current && !(await handleExpired(failure))) {
        setLoadError(
          failure instanceof ApiError && failure.status === 404
            ? 'Este pedido não está disponível para seu perfil.'
            : opportunityError(failure),
        );
      }
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [handleExpired, idPedido, token]);

  useFocusEffect(
    useCallback(() => {
      void load();
      return () => {
        generation.current++;
      };
    }, [load]),
  );

  async function submit() {
    if (!token || idPedido === null || quote || submitting.current) return;
    const parsed = parseQuoteValue(valor);
    const days = Number(prazo);
    const validation: Record<string, string> = {};
    if (parsed === null)
      validation.valor = valor.trim()
        ? 'Informe um valor maior que zero, com até duas casas decimais'
        : 'Informe o preço do serviço';
    if (!/^\d+$/.test(prazo) || !Number.isInteger(days) || days < 1 || days > 2_147_483_647)
      validation.prazoDias = 'Informe o prazo em dias inteiros, maior que zero';
    if (descricao.trim().length > 5000)
      validation.descricao = 'As observações devem ter no máximo 5000 caracteres';
    setErrors(validation);
    setMessage(null);
    if (Object.keys(validation).length) {
      setMessage('Confira os campos destacados para enviar o orçamento.');
      return;
    }
    const request = generation.current;
    submitting.current = true;
    setSaving(true);
    try {
      const created = await createQuote(token, idPedido, {
        valor: parsed!,
        prazoDias: days,
        descricao: descricao.trim() || null,
      });
      if (request === generation.current) {
        setQuote(created);
        setNotice(true);
      }
    } catch (failure) {
      if (request !== generation.current || (await handleExpired(failure))) return;
      if (failure instanceof ApiError && failure.status === 409) {
        await load();
        setMessage('Você já enviou um orçamento para este pedido.');
      } else {
        if (failure instanceof ApiError) setErrors(failure.fields);
        setMessage(
          failure instanceof Error ? failure.message : 'Não foi possível enviar o orçamento.',
        );
      }
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title={quote ? 'Seu orçamento' : 'Enviar orçamento'}
        subtitle={title || undefined}
        onBack={() => {
          if (!submitting.current) router.dismissTo(`/prestador/oportunidades/${id}`);
        }}
      />
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {loading ? (
          <View style={styles.loading}>
            <ActivityIndicator accessibilityLabel="Carregando orçamento" />
          </View>
        ) : loadError ? (
          <ScreenState
            kind="error"
            title="Orçamento indisponível"
            message={loadError}
            actionLabel="Tentar novamente"
            onAction={load}
          />
        ) : quote ? (
          <>
            <View
              style={[
                styles.summary,
                {
                  borderColor: colors.outlineVariant,
                  backgroundColor: colors.surface,
                },
              ]}
            >
              <Text variant="labelLarge" style={{ color: colors.primary }}>
                {quoteStatusLabel(quote.status)}
              </Text>
              <Text variant="headlineSmall">{formatMoney(quote.valor)}</Text>
              <Text variant="bodyLarge">Prazo de execução: {formatDeadline(quote.prazoDias)}</Text>
              {quote.descricao && (
                <>
                  <Text variant="labelLarge">Observações</Text>
                  <Text variant="bodyLarge" selectable>
                    {quote.descricao}
                  </Text>
                </>
              )}
            </View>
            <InfoBanner icon="info">
              Seu orçamento está disponível para consulta pelo cliente.
            </InfoBanner>
            <Button mode="contained" disabled>
              Orçamento enviado
            </Button>
          </>
        ) : (
          <>
            <TextField
              label="Valor do serviço"
              placeholder="R$ 0,00"
              value={valor}
              keyboardType="decimal-pad"
              maxLength={20}
              editable={!saving}
              error={errors.valor}
              onChangeText={(value) => {
                setValor(value);
                setErrors((current) => ({ ...current, valor: '' }));
              }}
            />
            <TextField
              label="Prazo de execução (dias)"
              placeholder="Ex.: 2"
              value={prazo}
              keyboardType="number-pad"
              maxLength={10}
              editable={!saving}
              error={errors.prazoDias}
              onChangeText={(value) => {
                setPrazo(value);
                setErrors((current) => ({ ...current, prazoDias: '' }));
              }}
            />
            <TextField
              label="Observações (opcional)"
              placeholder="O que está incluso, materiais, garantia..."
              value={descricao}
              multiline
              maxLength={5000}
              editable={!saving}
              error={errors.descricao}
              onChangeText={(value) => {
                setDescricao(value);
                setErrors((current) => ({ ...current, descricao: '' }));
              }}
            />
            {message ? (
              <AlertBanner>{message}</AlertBanner>
            ) : (
              <InfoBanner icon="info">
                Informe o valor total e o prazo de execução do serviço para o cliente analisar sua
                proposta.
              </InfoBanner>
            )}
            <SubmitButton loading={saving} disabled={saving} onPress={submit}>
              {saving ? 'Enviando...' : 'Enviar orçamento'}
            </SubmitButton>
          </>
        )}
        {quote && message && <AlertBanner>{message}</AlertBanner>}
      </ScrollView>
      <Snackbar visible={notice} onDismiss={() => setNotice(false)}>
        Orçamento enviado ao cliente
      </Snackbar>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingTop: 4, gap: 16, paddingBottom: 32 },
  loading: { padding: 48, alignItems: 'center' },
  summary: { padding: 16, gap: 12, borderWidth: 1, borderRadius: 12 },
});
