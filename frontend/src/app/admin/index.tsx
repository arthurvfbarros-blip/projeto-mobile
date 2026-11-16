import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Chip, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { approvalLabels, listProviders, type ProviderPage } from '@/services/admin';
import { ApiError } from '@/services/api';
import type { ApprovalStatus } from '@/services/providers';

export default function AdminScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession, signOut } = useSession();
  const token = session?.accessToken;
  const [status, setStatus] = useState<ApprovalStatus>('pendente');
  const [pagina, setPagina] = useState(0);
  const [data, setData] = useState<ProviderPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [leaveError, setLeaveError] = useState<string | null>(null);
  const [leaving, setLeaving] = useState(false);
  const generation = useRef(0);

  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setError(null);
    setData(null);
    try {
      const response = await listProviders(token, status, pagina);
      if (request === generation.current) setData(response);
    } catch (e) {
      if (request !== generation.current) return;
      if (e instanceof ApiError && e.status === 401) {
        await expireSession(token).catch(() => setError('Entre novamente para continuar.'));
      } else {
        setError(e instanceof Error ? e.message : 'Não foi possível carregar os prestadores.');
      }
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [token, status, pagina, expireSession]);

  useFocusEffect(useCallback(() => {
    void load();
    return () => { generation.current++; };
  }, [load]));

  async function leave() {
    setLeaving(true);
    setLeaveError(null);
    try { await signOut(); }
    catch { setLeaveError('Não foi possível sair. Tente novamente.'); setLeaving(false); }
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader variant="app" title="Administração" subtitle="Análise de prestadores" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
          Confira os serviços e a região de atendimento antes de decidir sobre o cadastro.
        </Text>
        <View style={styles.filters}>
          {(['pendente', 'aprovado', 'reprovado'] as const).map(value => (
            <Chip key={value} selected={status === value} showSelectedCheck={false} disabled={leaving}
              mode="outlined"
              style={{
                backgroundColor: status === value ? colors.primaryContainer : colors.surfaceContainerLowest,
                borderColor: status === value ? colors.primary : colors.outlineVariant,
              }}
              accessibilityLabel={`Mostrar cadastros: ${approvalLabels[value]}`}
              onPress={() => { setStatus(value); setPagina(0); }}>
              {approvalLabels[value]}
            </Chip>
          ))}
        </View>
        <Button mode="outlined" disabled={loading || leaving} onPress={load}>Atualizar lista</Button>
        {loading ? <ActivityIndicator accessibilityLabel="Carregando prestadores" /> : error ? (
          <ScreenState kind="error" title="Não foi possível carregar" message={error}
            actionLabel="Tentar novamente" onAction={load} />
        ) : data && (
          <>
            <Text variant="labelLarge">{data.totalItens} cadastro(s)</Text>
            {data.itens.length === 0 ? (
              <ScreenState kind="empty" title="Nenhum cadastro nesta página"
                message="Novos cadastros aparecem na aba Pendente. Atualize para consultar novamente." />
            ) : data.itens.map(({ perfil: p, ativo }) => (
              <View key={p.idPrestador} style={[styles.card, { borderColor: colors.outlineVariant }]}>
                <Text variant="titleMedium" accessibilityRole="header">{p.nome}</Text>
                <Text variant="bodyMedium">{p.cidade && p.uf ? `${p.cidade} / ${p.uf}` : 'Região não informada'}</Text>
                <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                  {p.categorias.map(c => c.nome).join(', ') || 'Sem categorias cadastradas'}
                </Text>
                {!ativo && <Text style={{ color: colors.error }}>Conta inativa</Text>}
                <Button mode="outlined" disabled={leaving}
                  accessibilityLabel={`Ver cadastro de ${p.nome}`}
                  onPress={() => router.push({ pathname: '/admin/prestadores/[id]', params: { id: p.idPrestador } })}>
                  Ver cadastro
                </Button>
              </View>
            ))}
            <View style={styles.pagination}>
              <Button disabled={pagina === 0 || leaving} onPress={() => setPagina(p => p - 1)}>Anterior</Button>
              <Text>Página {pagina + 1} de {Math.max(1, data.totalPaginas)}</Text>
              <Button disabled={pagina + 1 >= data.totalPaginas || leaving}
                onPress={() => setPagina(p => p + 1)}>Próxima</Button>
            </View>
          </>
        )}
        {leaveError && <AlertBanner>{leaveError}</AlertBanner>}
        <Button loading={leaving} disabled={leaving} onPress={leave}>Sair da conta</Button>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, gap: 16, width: '100%', maxWidth: 560, alignSelf: 'center' },
  filters: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, gap: 10 },
  pagination: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap' },
});
