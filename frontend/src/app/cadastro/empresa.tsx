import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { ArrowRightIcon } from '@/components/arrow-right-icon';
import { Checkbox } from '@/components/checkbox';
import { FormField } from '@/components/form-field';
import { ScreenHeader } from '@/components/screen-header';
import { SignUpProgress } from '@/components/signup-progress';
import { useAppTheme } from '@/constants/theme';
import { ApiError, NetworkError } from '@/services/api';
import { signUpCompany } from '@/services/signup';
import { formatCnpj, formatCpf, formatPhone, onlyCnpjChars, onlyDigits } from '@/utils/masks';
import {
  cnpjError,
  cpfError,
  emailError,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordError,
  phoneError,
  requiredError,
} from '@/utils/validators';

type Form = {
  razaoSocial: string;
  nomeFantasia: string;
  cnpj: string;
  email: string;
  telefone: string;
  endereco: string;
  nomeResponsavel: string;
  cpfResponsavel: string;
  senha: string;
};

type Errors = Partial<Record<keyof Form | 'termos', string>>;

function validate(form: Form, acceptedTerms: boolean): Errors {
  return {
    razaoSocial: requiredError(form.razaoSocial, 'Informe a razão social'),
    nomeFantasia: requiredError(form.nomeFantasia, 'Informe o nome fantasia'),
    cnpj: cnpjError(form.cnpj),
    email: emailError(form.email),
    telefone: phoneError(form.telefone, 'telefone'),
    endereco: requiredError(form.endereco, 'Informe a sede e a área de atendimento'),
    nomeResponsavel: requiredError(form.nomeResponsavel, 'Informe o nome do responsável'),
    cpfResponsavel: cpfError(form.cpfResponsavel),
    senha: passwordError(form.senha),
    termos: acceptedTerms ? undefined : 'Aceite os termos para continuar',
  };
}

export default function CompanySignUpScreen() {
  const { colors } = useAppTheme();
  const [form, setForm] = useState<Form>({
    razaoSocial: '',
    nomeFantasia: '',
    cnpj: '',
    email: '',
    telefone: '',
    endereco: '',
    nomeResponsavel: '',
    cpfResponsavel: '',
    senha: '',
  });
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
      await signUpCompany({
        razaoSocial: form.razaoSocial.trim(),
        nomeFantasia: form.nomeFantasia.trim(),
        cnpj: onlyCnpjChars(form.cnpj),
        email: form.email.trim(),
        telefone: onlyDigits(form.telefone),
        endereco: form.endereco.trim(),
        nomeResponsavel: form.nomeResponsavel.trim(),
        cpfResponsavel: onlyDigits(form.cpfResponsavel),
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
        setBannerMessage('Não foi possível criar a conta da empresa. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        title="Cadastrar empresa"
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/cadastro'))}
      />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets>
        <SignUpProgress label="Cadastro de empresa" step={2} totalSteps={2} />

        <View style={styles.presentation}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Conte sobre seu negócio
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Esses dados ajudam clientes a contratar sua empresa com confiança.
          </Text>
        </View>

        <View style={styles.section}>
          <SectionTitle number={1} title="Dados da empresa" />
          <FormField
            label="Razão social"
            icon={require('@/assets/icons/field-building.svg')}
            placeholder="Nome registrado da empresa"
            value={form.razaoSocial}
            onChangeText={(value) => update('razaoSocial', value)}
            error={errors.razaoSocial}
            editable={!loading}
            autoCapitalize="words"
            autoComplete="organization"
            textContentType="organizationName"
          />
          <FormField
            label="Nome fantasia"
            icon={require('@/assets/icons/field-store.svg')}
            placeholder="Como os clientes conhecem você"
            value={form.nomeFantasia}
            onChangeText={(value) => update('nomeFantasia', value)}
            error={errors.nomeFantasia}
            editable={!loading}
            autoCapitalize="words"
          />
          <FormField
            label="CNPJ"
            icon={require('@/assets/icons/field-file-text.svg')}
            placeholder="00.000.000/0000-00"
            value={form.cnpj}
            onChangeText={(value) => update('cnpj', formatCnpj(value))}
            error={errors.cnpj}
            editable={!loading}
            autoCapitalize="characters"
            autoCorrect={false}
          />
          <FormField
            label="E-mail comercial"
            icon={require('@/assets/icons/field-mail.svg')}
            placeholder="contato@suaempresa.com"
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
            label="Telefone comercial"
            icon={require('@/assets/icons/field-phone.svg')}
            placeholder="(11) 3333-3333"
            value={form.telefone}
            onChangeText={(value) => update('telefone', formatPhone(value))}
            error={errors.telefone}
            editable={!loading}
            keyboardType="phone-pad"
            autoComplete="tel"
            textContentType="telephoneNumber"
          />
          <FormField
            label="Sede e área de atendimento"
            icon={require('@/assets/icons/field-map-pin.svg')}
            placeholder="CEP, cidade e estado"
            value={form.endereco}
            onChangeText={(value) => update('endereco', value)}
            error={errors.endereco}
            editable={!loading}
            autoComplete="postal-address"
            textContentType="fullStreetAddress"
          />
        </View>

        <View style={[styles.separator, { backgroundColor: colors.surfaceVariant }]} />

        <View style={styles.section}>
          <SectionTitle number={2} title="Responsável pela conta" />
          <FormField
            label="Nome completo"
            icon={require('@/assets/icons/field-user.svg')}
            placeholder="Nome do responsável"
            value={form.nomeResponsavel}
            onChangeText={(value) => update('nomeResponsavel', value)}
            error={errors.nomeResponsavel}
            editable={!loading}
            autoCapitalize="words"
            autoComplete="name"
            textContentType="name"
          />
          <FormField
            label="CPF"
            icon={require('@/assets/icons/field-badge-check.svg')}
            placeholder="000.000.000-00"
            value={form.cpfResponsavel}
            onChangeText={(value) => update('cpfResponsavel', formatCpf(value))}
            error={errors.cpfResponsavel}
            editable={!loading}
            keyboardType="number-pad"
          />
          <FormField
            label="Senha de acesso"
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

        <View style={[styles.notice, { backgroundColor: colors.primaryContainer }]}>
          <Text variant="labelLarge" style={{ color: colors.onPrimaryContainer }}>
            Verificação rápida
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Após o cadastro, validaremos o CNPJ para liberar a publicação de serviços.
          </Text>
        </View>

        <Checkbox
          label="Li e aceito os Termos de uso e a Política de privacidade."
          checked={acceptedTerms}
          onChange={handleAcceptTerms}
          error={errors.termos}
          disabled={loading}
        />

        <View style={styles.actions}>
          <Button
            mode="contained"
            icon={ArrowRightIcon}
            onPress={handleSubmit}
            loading={loading}
            contentStyle={styles.buttonContent}>
            {loading ? 'Criando conta...' : 'Criar conta da empresa'}
          </Button>

          {bannerMessage && <AlertBanner>{bannerMessage}</AlertBanner>}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function SectionTitle({ number, title }: { number: number; title: string }) {
  const { colors } = useAppTheme();

  return (
    <View style={styles.sectionTitle}>
      <View style={[styles.sectionNumber, { backgroundColor: colors.primary }]}>
        <Text variant="labelMedium" style={{ color: colors.onPrimary }}>
          {number}
        </Text>
      </View>
      <Text variant="titleMedium" accessibilityRole="header" style={styles.sectionTitleText}>
        {title}
      </Text>
    </View>
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
  section: {
    gap: 16,
  },
  sectionTitle: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  sectionNumber: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 24,
    height: 24,
    borderRadius: 999,
  },
  sectionTitleText: {
    flex: 1,
  },
  separator: {
    height: 1,
  },
  notice: {
    gap: 8,
    padding: 16,
    borderRadius: 12,
  },
  actions: {
    gap: 12,
  },
  buttonContent: {
    flexDirection: 'row-reverse',
    height: 52,
  },
});
