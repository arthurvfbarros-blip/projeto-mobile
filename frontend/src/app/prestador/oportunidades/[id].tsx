import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { getOpportunity, opportunityError, type Oportunidade } from '@/services/opportunities';
import { formatCreatedAt, formatOrderDate, orderStatusLabel } from '@/utils/order-validation';

export default function OpportunityDetailsScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [data, setData] = useState<Oportunidade | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [accessDenied, setAccessDenied] = useState(false);
  const generation = useRef(0);
  const token = session?.accessToken;

  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setData(null);
    setError(null);
    setNotFound(false);
    setAccessDenied(false);
    const idPedido = Number(id);
    if (
      !/^\d+$/.test(id ?? '') ||
      !Number.isInteger(idPedido) ||
      idPedido <= 0 ||
      idPedido > 2_147_483_647
    ) {
      setNotFound(true);
      setLoading(false);
      return;
    }
    try {
      const response = await getOpportunity(token, idPedido);
      if (request === generation.current) setData(response);
    } catch (failure) {
      if (request !== generation.current) return;
      if (failure instanceof ApiError && failure.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else if (failure instanceof ApiError && failure.status === 404) {
        setNotFound(true);
      } else {
        setAccessDenied(failure instanceof ApiError && failure.status === 403);
        setError(opportunityError(failure));
      }
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, id, token]);

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
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title="Detalhes do pedido"
        onBack={() => router.dismissTo('/prestador/oportunidades')}
      />
      <ScrollView contentContainerStyle={styles.content}>
        {loading ? (
          <View style={styles.state}>
            <ActivityIndicator accessibilityLabel="Carregando oportunidade" />
            <Text>Carregando oportunidade...</Text>
          </View>
        ) : notFound ? (
          <>
            <Text variant="headlineSmall" accessibilityRole="header">
              Oportunidade não encontrada
            </Text>
            <Text>Este pedido não está disponível para seu perfil.</Text>
            <Button mode="outlined" onPress={() => router.dismissTo('/prestador/oportunidades')}>
              Voltar às oportunidades
            </Button>
          </>
        ) : error ? (
          <>
            <AlertBanner>{error}</AlertBanner>
            {accessDenied ? (
              <Button mode="outlined" onPress={() => router.dismissTo('/prestador')}>
                Conferir cadastro
              </Button>
            ) : (
              <Button mode="outlined" onPress={load}>
                Tentar novamente
              </Button>
            )}
          </>
        ) : data ? (
          <>
            <View style={styles.presentation}>
              <Text variant="headlineSmall" accessibilityRole="header">
                {data.titulo}
              </Text>
              <Text variant="labelLarge" style={{ color: colors.primary }}>
                {orderStatusLabel(data.status)}
              </Text>
              <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                Publicado em: {formatCreatedAt(data.criadoEm)}
              </Text>
            </View>
            <View
              style={[
                styles.card,
                { borderColor: colors.outlineVariant, backgroundColor: colors.surface },
              ]}>
              <Text variant="labelLarge">Categoria</Text>
              <Text variant="bodyLarge">{data.categoria.nome}</Text>
              <Text variant="labelLarge">Descrição do serviço</Text>
              <Text variant="bodyLarge" selectable>
                {data.descricao}
              </Text>
              <Text variant="labelLarge">Região do atendimento</Text>
              <Text variant="bodyLarge">
                {data.bairro ? data.bairro + ' · ' : ''}
                {data.cidade} / {data.uf}
              </Text>
              <Text variant="labelLarge">Data desejada</Text>
              <Text variant="bodyLarge">{formatOrderDate(data.dataDesejada)}</Text>
            </View>
            <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
              A data desejada é uma preferência. O agendamento será confirmado em outra etapa.
            </Text>
          </>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    padding: 16,
    paddingBottom: 32,
    gap: 16,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  state: { paddingVertical: 32, alignItems: 'center', gap: 16 },
  presentation: { gap: 8 },
  card: { padding: 16, gap: 12, borderWidth: 1, borderRadius: 12 },
});
