import { Roboto_400Regular } from '@expo-google-fonts/roboto/400Regular';
import { Roboto_500Medium } from '@expo-google-fonts/roboto/500Medium';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Platform, StyleSheet, View } from 'react-native';
import { ActivityIndicator, PaperProvider } from 'react-native-paper';

import { theme } from '@/constants/theme';
import { SessionProvider, useSession } from '@/providers/session-provider';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({ Roboto_400Regular, Roboto_500Medium });
  const ready = fontsLoaded || !!fontError;

  useEffect(() => {
    if (ready) SplashScreen.hide();
  }, [ready]);

  if (!ready) return null;

  return (
    <View style={styles.viewport}>
      <View style={styles.appFrame}>
        <PaperProvider theme={theme}>
          <SessionProvider>
            <StatusBar style="dark" />
            <RootNavigation />
          </SessionProvider>
        </PaperProvider>
      </View>
    </View>
  );
}

function RootNavigation() {
  const { session, loading } = useSession();
  if (loading) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator accessibilityLabel="Carregando sua conta" />
      </View>
    );
  }
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="login" />
      <Stack.Protected guard={session?.usuario.tipoUsuario === 'cliente'}>
        <Stack.Screen name="cliente" />
      </Stack.Protected>
      <Stack.Protected guard={session?.usuario.tipoUsuario === 'prestador'}>
        <Stack.Screen name="prestador" />
      </Stack.Protected>
    </Stack>
  );
}

const styles = StyleSheet.create({
  viewport: { flex: 1, backgroundColor: theme.colors.surfaceVariant },
  appFrame: {
    flex: 1,
    width: '100%',
    backgroundColor: theme.colors.surfaceContainerLowest,
    overflow: 'hidden',
    ...Platform.select({ web: { maxWidth: 430, alignSelf: 'center' as const } }),
  },
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.colors.surfaceContainerLowest,
  },
});
