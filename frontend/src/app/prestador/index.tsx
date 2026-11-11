import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { getProviderProfile, type ProviderProfile } from '@/services/providers';

const approvalLabels = {
  pendente: 'Cadastro em análise',
  aprovado: 'Cadastro aprovado',
  reprovado: 'Cadastro reprovado',
};

export default function ProviderHomeScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession, signOut } = useSession();
  const [profile, setProfile] = useState<ProviderProfile | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [signOutError, setSignOutError] = useState<string | null>(null);
  const [signingOut, setSigningOut] = useState(false);
  const generation = useRef(0);
  const token = session?.accessToken;

  const loadProfile = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setProfile(null);
    setLoadError(null);
    try {
      const data = await getProviderProfile(token);
      if (request === generation.current) setProfile(data);
    } catch (error) {
      if (request !== generation.current) return;
      if (error instanceof ApiError && error.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else {
        setLoadError(
          error instanceof Error ? error.message : 'Não foi possível carregar o perfil.',
        );
      }
    }
  }, [expireSession, token]);

  useFocusEffect(
    useCallback(() => {
      void loadProfile();
      return () => {
        generation.current++;
      };
    }, [loadProfile]),
  );

  async function leave() {
    setSigningOut(true);
    setSignOutError(null);
    try {
      await signOut();
    } catch {
      setSignOutError('Não foi possível sair da conta. Tente novamente.');
      setSigningOut(false);
    }
  }

  return (
    <SafeAreaView
      edges={['top', 'left', 'right']}
      style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader variant="app" title="Área do prestador" />
      <ScrollView contentContainerStyle={styles.content}>
        {loadError ? (
          <>
            <AlertBanner>{loadError}</AlertBanner>
            <Button mode="outlined" onPress={loadProfile}>
              Tentar novamente
            </Button>
          </>
        ) : profile ? (
          <>
            <Text variant="headlineSmall" accessibilityRole="header">
              Olá, {profile.nome}
            </Text>
            <View
              style={[
                styles.card,
                {
                  borderColor: colors.outlineVariant,
                  backgroundColor:
                    profile.statusAprovacao === 'reprovado'
                      ? colors.errorContainer
                      : colors.primaryContainer,
                },
              ]}>
              <Text variant="titleMedium" accessibilityRole="header">
                {approvalLabels[profile.statusAprovacao]}
              </Text>
              {profile.statusAprovacao === 'pendente' && (
                <Text variant="bodyMedium">A equipe está analisando seu perfil profissional.</Text>
              )}
            </View>
            <View style={[styles.card, { borderColor: colors.outlineVariant }]}>
              <Text variant="titleMedium">Serviços oferecidos</Text>
              <Text variant="bodyMedium">
                {profile.categorias.map((categoria) => categoria.nome).join(', ') ||
                  'Nenhuma categoria cadastrada'}
              </Text>
              <Text variant="titleMedium">Área de atendimento</Text>
              <Text variant="bodyMedium">
                {profile.cidade && profile.uf
                  ? profile.cidade + ' / ' + profile.uf
                  : 'Não informada'}
              </Text>
              {profile.cidade && profile.uf && (
                <Text variant="bodyMedium">
                  {profile.bairros.length ? profile.bairros.join(', ') : 'Toda a cidade'}
                </Text>
              )}
            </View>
            {profile.statusAprovacao === 'aprovado' && (
              <Button
                mode="contained"
                disabled={signingOut}
                onPress={() => router.push('/prestador/oportunidades')}>
                Ver oportunidades
              </Button>
            )}
            <Button mode="outlined" onPress={loadProfile} disabled={signingOut}>
              Atualizar situação
            </Button>
          </>
        ) : (
          <ActivityIndicator accessibilityLabel="Carregando perfil profissional" />
        )}
        {signOutError && <AlertBanner>{signOutError}</AlertBanner>}
        <Button mode="text" onPress={leave} loading={signingOut} disabled={signingOut}>
          Sair da conta
        </Button>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { width: '100%', maxWidth: 560, alignSelf: 'center', gap: 20, padding: 20 },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, gap: 8 },
});
