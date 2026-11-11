import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ArrowRightIcon } from '@/components/arrow-right-icon';
import { Checkbox } from '@/components/checkbox';
import { FormField } from '@/components/form-field';
import { InfoBanner } from '@/components/info-banner';
import { ScreenHeader } from '@/components/screen-header';
import { SignUpProgress } from '@/components/signup-progress';
import { SubmitButton } from '@/components/submit-button';
import { useAppTheme } from '@/constants/theme';
import { ApiError, NetworkError } from '@/services/api';
import { signUpUser } from '@/services/signup';
import { formatCpf, formatPhone, onlyDigits } from '@/utils/masks';
import {
  cpfError,
  emailError,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordError,
  phoneError,
  requiredError,
} from '@/utils/validators';

type Form = {
  nome: string;
  email: string;
  celular: string;
  cpf: string;
  senha: string;
};

type Errors = Partial<Record<keyof Form | 'termos', string>>;

function validate(form: Form, acceptedTerms: boolean): Errors {
  return {
    nome: requiredError(form.nome, 'Informe seu nome'),
    email: emailError(form.email),
    celular: phoneError(form.celular, 'celular'),
    cpf: cpfError(form.cpf),
    senha: passwordError(form.senha),
    termos: acceptedTerms ? undefined : 'Aceite os termos para continuar',
  };
}

export default function UserSignUpScreen() {
  const { colors } = useAppTheme();
  const [form, setForm] = useState<Form>({ nome: '', email: '', celular: '', cpf: '', senha: '' });
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [errors, setErrors] = useState<Errors>({});
  const [loading, setLoading] = useState(false);
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);

  function update(field: keyof Form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  function handleAcceptTerms(accepted: boolean) {
    setAcceptedTerms(accepted);
    setErrors((current) => ({ ...current, termos: undefined }));
  }

  async function handleSubmit() {
    if (loading) return;

    const validationErrors = validate(form, acceptedTerms);
    setErrors(validationErrors);
    setBannerMessage(null);
    if (Object.values(validationErrors).some(Boolean)) return;

    setLoading(true);
    try {
      await signUpUser({
        nome: form.nome.trim(),
        email: form.email.trim(),
        celular: onlyDigits(form.celular),
        cpf: onlyDigits(form.cpf),
        senha: form.senha,
      });
      router.dismissTo({ pathname: '/login', params: { contaCriada: '1' } });
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fields).length > 0) {
        setErrors(error.fields);
      } else if (error instanceof ApiError || error instanceof NetworkError) {
        setBannerMessage(error.message);
      } else {
        console.error(error);
        setBannerMessage('Não foi possível criar sua conta. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        title="Criar conta"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/cadastro'))}
      />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets>
        <SignUpProgress label="Cadastro de usuário" step={2} totalSteps={2} />

        <View style={styles.presentation}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Seus dados de acesso
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Preencha as informações essenciais. Leva menos de 2 minutos.
          </Text>
        </View>

        <View style={styles.fields}>
          <FormField
            label="Nome completo"
            icon={require('@/assets/icons/field-user.svg')}
            placeholder="Como você gostaria de ser chamado"
            value={form.nome}
            onChangeText={(value) => update('nome', value)}
            error={errors.nome}
            editable={!loading}
            autoCapitalize="words"
            autoComplete="name"
            textContentType="name"
          />
          <FormField
            label="E-mail"
            icon={require('@/assets/icons/field-mail.svg')}
            placeholder="voce@exemplo.com"
            value={form.email}
            onChangeText={(value) => update('email', value)}
            error={errors.email}
            editable={!loading}
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            textContentType="emailAddress"
          />
          <FormField
            label="Celular"
            icon={require('@/assets/icons/field-smartphone.svg')}
            placeholder="(11) 99999-9999"
            value={form.celular}
            onChangeText={(value) => update('celular', formatPhone(value))}
            error={errors.celular}
            editable={!loading}
            keyboardType="phone-pad"
            autoComplete="tel"
            textContentType="telephoneNumber"
          />
          <FormField
            label="CPF"
            icon={require('@/assets/icons/field-badge-check.svg')}
            placeholder="000.000.000-00"
            helperText="Usado somente para segurança e emissão de comprovantes."
            value={form.cpf}
            onChangeText={(value) => update('cpf', formatCpf(value))}
            error={errors.cpf}
            editable={!loading}
            keyboardType="number-pad"
          />
          <FormField
            label="Senha"
            icon={require('@/assets/icons/field-lock.svg')}
            placeholder={`Mínimo de ${PASSWORD_MIN_LENGTH} caracteres`}
            value={form.senha}
            onChangeText={(value) => update('senha', value)}
            error={errors.senha}
            editable={!loading}
            maxLength={PASSWORD_MAX_LENGTH}
            secureTextEntry
            autoComplete="new-password"
            textContentType="newPassword"
          />
        </View>

        <InfoBanner icon="lock">
          Seus dados são protegidos e não serão compartilhados sem autorização.
        </InfoBanner>

        <Checkbox
          label="Li e aceito os Termos de uso e a Política de privacidade."
          checked={acceptedTerms}
          onChange={handleAcceptTerms}
          error={errors.termos}
          disabled={loading}
        />

        <View style={styles.actions}>
          <SubmitButton
            icon={ArrowRightIcon}
            onPress={handleSubmit}
            loading={loading}
            disabled={loading}
            contentStyle={styles.buttonContent}>
            {loading ? 'Criando conta...' : 'Criar minha conta'}
          </SubmitButton>

          {bannerMessage ? (
            <AlertBanner>{bannerMessage}</AlertBanner>
          ) : (
            <View style={styles.signInRow}>
              <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                Já sou cadastrado
              </Text>
              <Button mode="text" onPress={() => router.dismissTo('/login')} disabled={loading}>
                Entrar
              </Button>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    gap: 24,
    paddingTop: 16,
    paddingBottom: 32,
    paddingHorizontal: 20,
  },
  presentation: {
    gap: 8,
  },
  fields: {
    gap: 16,
  },
  actions: {
    gap: 12,
  },
  buttonContent: {
    flexDirection: 'row-reverse',
    height: 52,
  },
  signInRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
  },
});
