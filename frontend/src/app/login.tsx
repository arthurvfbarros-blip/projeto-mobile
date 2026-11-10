import { Image } from 'expo-image';
import { Redirect, router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Snackbar, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { TextField } from '@/components/text-field';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError, NetworkError } from '@/services/api';

type FieldErrors = {
  email?: string;
  password?: string;
};

// US18 - Login (Figma: "Protótipo navegável 1", estados padrão, erro, carregando e vazio)
export default function LoginScreen() {
  const { colors } = useAppTheme();
  const { session, notice, authenticate, signOut, clearNotice } = useSession();
  const { contaCriada } = useLocalSearchParams<{ contaCriada?: string }>();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);

  const isEmpty = !email.trim() || !password;

  async function handleSignIn() {
    if (loading || isEmpty) return;

    setLoading(true);
    setFieldErrors({});
    setBannerMessage(null);
    clearNotice();
    try {
      const authenticated = await authenticate(email.trim(), password);
      if (authenticated.usuario.tipoUsuario === 'cliente') {
        router.replace('/cliente');
      } else {
        await signOut();
        setBannerMessage('Entre com uma conta de cliente para acessar seus endereços.');
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setFieldErrors({ password: error.message });
      } else if (error instanceof ApiError && Object.keys(error.fields).length > 0) {
        setFieldErrors({ email: error.fields.email, password: error.fields.senha });
      } else if (error instanceof ApiError || error instanceof NetworkError) {
        setBannerMessage(error.message);
      } else {
        console.error(error);
        setBannerMessage('Não foi possível entrar. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  if (session?.usuario.tipoUsuario === 'cliente') return <Redirect href="/cliente" />;

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets>
        <View style={[styles.iconContainer, { backgroundColor: colors.primaryContainer }]}>
          <Image source={require('@/assets/icons/login-home.svg')} style={styles.icon} />
        </View>

        <Text variant="headlineSmall">Entrar</Text>
        <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
          Acesse sua conta para pedir serviços
        </Text>

        <TextField
          label="E-mail"
          placeholder="seu@email.com"
          value={email}
          onChangeText={setEmail}
          editable={!loading}
          error={fieldErrors.email}
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
          textContentType="emailAddress"
          returnKeyType="next"
        />
        <TextField
          label="Senha"
          placeholder="Digite sua senha"
          value={password}
          onChangeText={setPassword}
          editable={!loading}
          error={fieldErrors.password}
          secureTextEntry
          autoComplete="current-password"
          textContentType="password"
          returnKeyType="go"
          onSubmitEditing={handleSignIn}
        />

        <View style={styles.forgotPasswordRow}>
          <Button mode="text">Esqueci minha senha</Button>
        </View>

        <Button
          mode="contained"
          onPress={handleSignIn}
          loading={loading}
          disabled={isEmpty || loading}
          labelStyle={isEmpty ? { color: colors.onSurfaceVariant } : undefined}>
          {loading ? 'Entrando...' : 'Entrar'}
        </Button>

        {(bannerMessage || notice) && <AlertBanner>{bannerMessage ?? notice!}</AlertBanner>}

        {!bannerMessage && !loading && (
          <View style={styles.signUpRow}>
            <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
              Não tem conta?
            </Text>
            <Button mode="text" onPress={() => router.push('/cadastro')}>
              Cadastre-se
            </Button>
          </View>
        )}
      </ScrollView>

      <Snackbar
        visible={contaCriada === '1'}
        onDismiss={() => router.setParams({ contaCriada: undefined })}>
        Conta criada! Agora é só entrar com seu e-mail e senha.
      </Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    gap: 16,
    paddingTop: 32,
    paddingBottom: 24,
    paddingHorizontal: 16,
  },
  iconContainer: {
    alignSelf: 'flex-start',
    padding: 16,
    borderRadius: 999,
  },
  icon: {
    width: 32,
    height: 32,
  },
  forgotPasswordRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  signUpRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
  },
});
