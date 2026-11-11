import { Image } from 'expo-image';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { listAddresses, type Endereco } from '@/services/addresses';
import { ApiError } from '@/services/api';
import { formatCep } from '@/utils/masks';

export default function AddressesScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const { enderecoCriado } = useLocalSearchParams<{ enderecoCriado?: string }>();
  const [addresses, setAddresses] = useState<Endereco[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const requestGeneration = useRef(0);
  const token = session?.accessToken;

  const loadAddresses = useCallback(async () => {
    if (!token) return;
    const generation = ++requestGeneration.current;
    setLoading(true);
    setError(null);
    setAddresses([]);
    try {
      const response = await listAddresses(token);
      if (generation === requestGeneration.current) setAddresses(response);
    } catch (failure) {
      if (generation !== requestGeneration.current) return;
      if (failure instanceof ApiError && failure.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else {
        setError(
          failure instanceof Error ? failure.message : 'Não foi possível carregar seus endereços.',
        );
      }
    } finally {
      if (generation === requestGeneration.current) setLoading(false);
    }
  }, [expireSession, token]);

  useFocusEffect(
    useCallback(() => {
      void loadAddresses();
      return () => {
        requestGeneration.current++;
      };
    }, [loadAddresses]),
  );

  return (
    <SafeAreaView
      edges={['top', 'left', 'right']}
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title="Meus endereços"
        onBack={() => router.dismissTo('/cliente')}
      />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.presentation}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Seus endereços
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Consulte seus locais de atendimento ou cadastre um novo endereço.
          </Text>
        </View>

        {loading ? (
          <View style={styles.state}>
            <ActivityIndicator accessibilityLabel="Carregando endereços" />
            <Text variant="bodyMedium">Carregando seus endereços...</Text>
          </View>
        ) : error ? (
          <View style={styles.actions}>
            <AlertBanner>{error}</AlertBanner>
            <Button
              mode="outlined"
              onPress={() => {
                void loadAddresses();
              }}>
              Tentar novamente
            </Button>
          </View>
        ) : addresses.length === 0 ? (
          <View style={[styles.empty, { borderColor: colors.outlineVariant }]}>
            <Image source={require('@/assets/icons/field-map-pin.svg')} style={styles.icon} />
            <Text variant="titleMedium" accessibilityRole="header">
              Você ainda não tem endereços
            </Text>
            <Text
              variant="bodyMedium"
              style={[styles.centered, { color: colors.onSurfaceVariant }]}>
              Cadastre seu primeiro endereço para usá-lo ao solicitar um serviço.
            </Text>
          </View>
        ) : (
          <View style={styles.cards}>
            {addresses.map((address) => (
              <View
                key={address.idEndereco}
                style={[styles.card, { borderColor: colors.outlineVariant }]}>
                <Text variant="titleMedium">
                  {address.logradouro}, {address.numero}
                </Text>
                {address.complemento ? (
                  <Text variant="bodyMedium">{address.complemento}</Text>
                ) : null}
                <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                  {address.bairro} · {address.cidade} / {address.uf}
                </Text>
                <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                  CEP {formatCep(address.cep)}
                </Text>
              </View>
            ))}
          </View>
        )}

        <View style={styles.actions}>
          <Button
            mode="contained"
            contentStyle={styles.button}
            onPress={() => router.push('/cliente/enderecos/novo')}>
            Cadastrar novo endereço
          </Button>
          {!loading && !error && addresses.length > 0 && (
            <Button
              mode="text"
              onPress={() => {
                void loadAddresses();
              }}>
              Atualizar lista
            </Button>
          )}
        </View>
      </ScrollView>
      <Snackbar
        visible={enderecoCriado === '1'}
        onDismiss={() => router.setParams({ enderecoCriado: undefined })}>
        Endereço cadastrado com sucesso!
      </Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    gap: 24,
    padding: 20,
    paddingBottom: 32,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  presentation: { gap: 8 },
  state: { gap: 16, paddingVertical: 32, alignItems: 'center' },
  empty: { gap: 12, padding: 24, borderWidth: 1, borderRadius: 12, alignItems: 'center' },
  icon: { width: 18, height: 18 },
  centered: { textAlign: 'center' },
  cards: { gap: 14 },
  card: { gap: 6, padding: 16, borderWidth: 1, borderRadius: 12 },
  actions: { gap: 12 },
  button: { height: 52 },
});
