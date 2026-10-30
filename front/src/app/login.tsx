import { Image } from 'expo-image';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { TextField } from '@/components/text-field';
import { useAppTheme } from '@/constants/theme';
import { InvalidCredentialsError, signIn } from '@/services/auth';

// US18 - Login (Figma: "Protótipo navegável 1", estados padrão, erro, carregando e vazio)
export default function LoginScreen() {
  const { colors } = useAppTheme();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [remainingAttempts, setRemainingAttempts] = useState<number | null>(null);

  const isEmpty = !email.trim() || !password;
  const hasError = remainingAttempts !== null;

  async function handleSignIn() {
    if (loading || isEmpty) return;

    setLoading(true);
    setRemainingAttempts(null);
    try {
      await signIn(email.trim(), password);
      // TODO: ir para a área do cliente quando a primeira tela dela existir.
    } catch (error) {
      if (!(error instanceof InvalidCredentialsError)) throw error;
      setRemainingAttempts(error.remainingAttempts);
    } finally {
      setLoading(false);
    }
  }

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
          error={hasError ? 'E-mail ou senha inválidos' : undefined}
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
          disabled={isEmpty}
          labelStyle={isEmpty ? { color: colors.onSurfaceVariant } : undefined}>
          {loading ? 'Entrando...' : 'Entrar'}
        </Button>

        {hasError && (
          <AlertBanner>
            {`Você tem mais ${remainingAttempts} ${remainingAttempts === 1 ? 'tentativa' : 'tentativas'} antes do bloqueio temporário de 15 minutos.`}
          </AlertBanner>
        )}

        {!hasError && !loading && (
          <View style={styles.signUpRow}>
            <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
              Não tem conta?
            </Text>
            <Button mode="text">Cadastre-se</Button>
          </View>
        )}
      </ScrollView>
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
