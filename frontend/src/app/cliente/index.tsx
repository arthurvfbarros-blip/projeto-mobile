import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ArrowRightIcon } from '@/components/arrow-right-icon';
import { ScreenHeader } from '@/components/screen-header';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';

export default function ClientHomeScreen() {
  const { colors } = useAppTheme();
  const { session, signOut } = useSession();
  const [leaving, setLeaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSignOut() {
    if (leaving) return;
    setLeaving(true);
    setError(null);
    try {
      await signOut();
    } catch {
      setError('Não foi possível sair da conta. Tente novamente.');
    } finally {
      setLeaving(false);
    }
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader title="Área do cliente" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.presentation}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Bem-vindo!
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            {session?.usuario.email}
          </Text>
        </View>

        <View style={[styles.card, { borderColor: colors.outlineVariant }]}>
          <View style={[styles.iconContainer, { backgroundColor: colors.primaryContainer }]}>
            <Image source={require('@/assets/icons/field-map-pin.svg')} style={styles.icon} />
          </View>
          <Text variant="titleLarge" accessibilityRole="header">
            Seus locais de atendimento
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Cadastre os endereços onde você costuma precisar de serviços.
          </Text>
          <Button
            mode="contained"
            icon={ArrowRightIcon}
            disabled={leaving}
            contentStyle={styles.button}
            onPress={() => router.push('/cliente/enderecos')}>
            Meus endereços
          </Button>
        </View>

        {error && <AlertBanner>{error}</AlertBanner>}
        <Button mode="text" onPress={handleSignOut} loading={leaving} disabled={leaving}>
          {leaving ? 'Saindo...' : 'Sair da conta'}
        </Button>
      </ScrollView>
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
  card: { gap: 16, borderWidth: 1, borderRadius: 16, padding: 20 },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  icon: { width: 18, height: 18 },
  button: { height: 52, flexDirection: 'row-reverse' },
});
