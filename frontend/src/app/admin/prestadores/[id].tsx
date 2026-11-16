import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Dialog, HelperText, Portal, Snackbar, Text, TextInput } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { approvalLabels, getProviderReview, reviewProvider, type ProviderReview } from '@/services/admin';
import { ApiError } from '@/services/api';

export default function ProviderReviewScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const providerId = /^\d+$/.test(id ?? '') ? Number(id) : NaN;
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const token = session?.accessToken;
  const [data, setData] = useState<ProviderReview | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [decision, setDecision] = useState<'aprovado' | 'reprovado'>('aprovado');
  const [dialogVisible, setDialogVisible] = useState(false);
  const [reason, setReason] = useState('');
  const [notice, setNotice] = useState('');
  const generation = useRef(0);
  const submitting = useRef(false);

  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setError(null);
    setData(null);
    setDialogVisible(false);
    try {
      if (!Number.isSafeInteger(providerId) || providerId <= 0) throw new Error('Cadastro não encontrado.');
      const response = await getProviderReview(token, providerId);
      if (request === generation.current) setData(response);
    } catch (e) {
      if (request !== generation.current) return;
      if (e instanceof ApiError && e.status === 401) {
        await expireSession(token).catch(() => setError('Entre novamente para continuar.'));
      } else setError(e instanceof Error ? e.message : 'Não foi possível carregar o cadastro.');
    } finally { if (request === generation.current) setLoading(false); }
  }, [token, providerId, expireSession]);

  useFocusEffect(useCallback(() => {
    void load();
    return () => { generation.current++; };
  }, [load]));

  async function submit() {
    if (!token || !dialogVisible || submitting.current || (decision === 'reprovado' && !reason.trim())) return;
    submitting.current = true;
    setSaving(true);
    setError(null);
    const request = generation.current;
    try {
      const response = await reviewProvider(token, providerId, decision, reason);
      if (request !== generation.current) return;
      setData(response);
      setDialogVisible(false);
      setReason('');
      setNotice('Análise registrada com sucesso.');
    } catch (e) {
      if (request !== generation.current) return;
      if (e instanceof ApiError && e.status === 401) {
        await expireSession(token).catch(() => setError('Entre novamente para continuar.'));
      } else {
        setError(e instanceof Error ? e.message : 'Não foi possível registrar a decisão.');
        setDialogVisible(false);
        if (e instanceof ApiError && e.status === 409) setData(null);
      }
    } finally { submitting.current = false; setSaving(false); }
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader variant="app" title="Analisar cadastro"
        onBack={saving ? undefined : () => router.replace('/admin')} />
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {loading ? <ActivityIndicator accessibilityLabel="Carregando cadastro" /> : data ? (
          <>
            <Text variant="headlineSmall" accessibilityRole="header">{data.perfil.nome}</Text>
            <Text variant="titleMedium" style={{ color: data.perfil.statusAprovacao === 'reprovado' ? colors.error : colors.primary }}>
              {approvalLabels[data.perfil.statusAprovacao]}
            </Text>
            <View style={[styles.card, { borderColor: colors.outlineVariant }]}>
              <Text variant="titleMedium">Contato</Text>
              <Text selectable>{data.email}</Text>
              <Text selectable>{data.telefone || 'Telefone não informado'}</Text>
              <Text>Conta {data.ativo ? 'ativa' : 'inativa'}</Text>
            </View>
            <View style={[styles.card, { borderColor: colors.outlineVariant }]}>
              <Text variant="titleMedium">Serviços oferecidos</Text>
              <Text>{data.perfil.categorias.map(c => c.nome).join(', ') || 'Nenhuma categoria cadastrada'}</Text>
              <Text variant="titleMedium">Área de atendimento</Text>
              <Text>{data.perfil.cidade && data.perfil.uf ? `${data.perfil.cidade} / ${data.perfil.uf}` : 'Não informada'}</Text>
              <Text>{data.perfil.bairros.length ? data.perfil.bairros.join(', ') : 'Toda a cidade'}</Text>
            </View>
            {data.analise && (
              <View style={[styles.card, { borderColor: colors.outlineVariant }]}>
                <Text variant="titleMedium">Registro da análise</Text>
                <Text>{data.analise.responsavel}</Text>
                <Text>{new Date(data.analise.analisadaEm).toLocaleString('pt-BR')}</Text>
                {data.analise.motivo && <Text>Motivo: {data.analise.motivo}</Text>}
              </View>
            )}
            {data.perfil.statusAprovacao === 'pendente' ? (
              <>
                {!data.ativo && <AlertBanner>A conta precisa estar ativa para receber uma decisão.</AlertBanner>}
                <Button mode="contained" disabled={saving || !data.ativo}
                  onPress={() => { setError(null); setDecision('aprovado'); setDialogVisible(true); }}>Aprovar cadastro</Button>
                <Button mode="outlined" textColor={colors.error} disabled={saving || !data.ativo}
                  onPress={() => { setError(null); setDecision('reprovado'); setDialogVisible(true); }}>Reprovar cadastro</Button>
              </>
            ) : <Text>A análise deste cadastro já foi concluída.</Text>}
          </>
        ) : <ScreenState kind="error" title="Cadastro indisponível"
          message="Atualize para consultar a situação atual do cadastro."
          actionLabel="Atualizar cadastro" onAction={load} />}
        {error && <AlertBanner>{error}</AlertBanner>}
        {data && <Button disabled={saving || loading} onPress={load}>Atualizar cadastro</Button>}
      </ScrollView>
      <Portal>
        <Dialog visible={dialogVisible} dismissable={!saving}
          onDismiss={() => { if (!saving) setDialogVisible(false); }} style={styles.dialog}>
          <Dialog.Title>{decision === 'aprovado' ? 'Aprovar cadastro?' : 'Reprovar cadastro?'}</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium" style={styles.dialogText}>
              {decision === 'aprovado'
                ? 'O prestador poderá consultar oportunidades compatíveis e enviar orçamentos.'
                : 'Informe um motivo claro. Ele será exibido ao prestador e o cadastro continuará sem acesso às oportunidades.'}
            </Text>
            {decision === 'reprovado' && (
              <>
                <TextInput mode="outlined" label="Motivo da reprovação" accessibilityLabel="Motivo da reprovação" value={reason}
                  onChangeText={setReason} multiline maxLength={500} disabled={saving} />
                <HelperText type="info">{reason.length}/500 caracteres</HelperText>
              </>
            )}
          </Dialog.Content>
          <Dialog.Actions>
            <Button disabled={saving} onPress={() => setDialogVisible(false)}>Voltar</Button>
            <Button loading={saving} disabled={saving || (decision === 'reprovado' && !reason.trim())}
              onPress={submit}>Confirmar</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
      <Snackbar visible={!!notice} onDismiss={() => setNotice('')}>{notice}</Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, gap: 16, width: '100%', maxWidth: 560, alignSelf: 'center' },
  card: { padding: 16, gap: 8, borderWidth: 1, borderRadius: 12 },
  dialog: { width: '90%', maxWidth: 400, alignSelf: 'center' },
  dialogText: { marginBottom: 12 },
});
