import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { Checkbox } from '@/components/checkbox';
import { ScreenHeader } from '@/components/screen-header';
import { SignUpProgress } from '@/components/signup-progress';
import { TextField } from '@/components/text-field';
import { useAppTheme } from '@/constants/theme';
import { ApiError, NetworkError } from '@/services/api';
import { listCategories, type Categoria } from '@/services/categories';
import { signUpProvider } from '@/services/providers';
import { ufError } from '@/utils/address-validation';
import { formatCnpj, formatCpf, formatPhone, onlyCnpjChars, onlyDigits } from '@/utils/masks';
import {
  cnpjError,
  cpfError,
  emailError,
  passwordError,
  PASSWORD_MAX_LENGTH,
  requiredError,
  phoneError,
} from '@/utils/validators';

const emptyForm = {
  nome: '',
  email: '',
  cpf: '',
  telefone: '',
  senha: '',
  cidade: '',
  uf: '',
  bairros: '',
  'empresa.razaoSocial': '',
  'empresa.nomeFantasia': '',
  'empresa.cnpj': '',
};

type Field = keyof typeof emptyForm;
type Errors = Partial<Record<Field | 'idCategorias' | 'termos', string>>;
type Catalog =
  | { state: 'loading' }
  | { state: 'loaded'; categories: Categoria[] }
  | { state: 'error'; message: string };

export default function ProviderSignUpScreen() {
  const { colors } = useAppTheme();
  const [form, setForm] = useState(emptyForm);
  const [categoryIds, setCategoryIds] = useState<number[]>([]);
  const [hasCompany, setHasCompany] = useState(false);
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [errors, setErrors] = useState<Errors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [catalog, setCatalog] = useState<Catalog>({ state: 'loading' });
  const [attempt, setAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const active = useRef(false);
  const submitting = useRef(false);

  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
    };
  }, []);

  useEffect(() => {
    let current = true;
    listCategories()
      .then((categories) => {
        if (current) setCatalog({ state: 'loaded', categories });
      })
      .catch((error: unknown) => {
        if (current) {
          setCatalog({
            state: 'error',
            message:
              error instanceof Error ? error.message : 'Não foi possível carregar as categorias.',
          });
        }
      });
    return () => {
      current = false;
    };
  }, [attempt]);

  function retryCatalog() {
    setCatalog({ state: 'loading' });
    setAttempt((value) => value + 1);
  }

  function update(field: Field, value: string) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setErrors((previous) => ({ ...previous, [field]: undefined }));
  }

  function toggleCategory(id: number, checked: boolean) {
    setCategoryIds((previous) =>
      checked ? [...previous, id] : previous.filter((item) => item !== id),
    );
    setErrors((previous) => ({ ...previous, idCategorias: undefined }));
  }

  function toggleCompany(checked: boolean) {
    setHasCompany(checked);
    setErrors((previous) => ({
      ...previous,
      'empresa.razaoSocial': undefined,
      'empresa.nomeFantasia': undefined,
      'empresa.cnpj': undefined,
    }));
  }

  async function submit() {
    if (submitting.current || catalog.state !== 'loaded') return;
    setMessage(null);
    const selected = catalog.categories
      .filter((category) => categoryIds.includes(category.idCategoria))
      .map((category) => category.idCategoria);
    const bairros = form.bairros
      .split(',')
      .map((bairro) => bairro.trim())
      .filter(Boolean);
    const validation: Errors = {
      nome: requiredError(form.nome, 'Informe seu nome'),
      email: emailError(form.email),
      cpf: cpfError(form.cpf),
      telefone: phoneError(form.telefone, 'telefone'),
      senha: passwordError(form.senha),
      cidade: requiredError(form.cidade, 'Informe a cidade'),
      uf: ufError(form.uf),
      termos: acceptedTerms ? undefined : 'Aceite os termos para continuar',
    };
    if (!selected.length) validation.idCategorias = 'Selecione ao menos uma categoria';
    else if (selected.length > 50) validation.idCategorias = 'Selecione até 50 categorias';
    if (bairros.length > 30) validation.bairros = 'Informe até 30 bairros';
    else if (bairros.some((bairro) => bairro.length > 100)) {
      validation.bairros = 'Cada bairro deve ter no máximo 100 caracteres';
    }
    if (hasCompany) {
      validation['empresa.razaoSocial'] = requiredError(
        form['empresa.razaoSocial'],
        'Informe a razão social',
      );
      validation['empresa.cnpj'] = cnpjError(form['empresa.cnpj']);
    }
    setErrors(validation);
    if (Object.values(validation).some(Boolean)) {
      setMessage('Confira os campos destacados.');
      return;
    }

    submitting.current = true;
    setSaving(true);
    try {
      await signUpProvider({
        nome: form.nome.trim(),
        email: form.email.trim(),
        cpf: onlyDigits(form.cpf),
        telefone: onlyDigits(form.telefone),
        senha: form.senha,
        idCategorias: selected,
        cidade: form.cidade.trim(),
        uf: form.uf.trim().toUpperCase(),
        bairros,
        ...(hasCompany && {
          empresa: {
            razaoSocial: form['empresa.razaoSocial'].trim(),
            nomeFantasia: form['empresa.nomeFantasia'].trim() || null,
            cnpj: onlyCnpjChars(form['empresa.cnpj']),
          },
        }),
      });
      if (active.current) {
        router.dismissTo({ pathname: '/login', params: { prestadorCadastrado: '1' } });
      }
    } catch (error) {
      if (!active.current) return;
      if (error instanceof ApiError) {
        const fields: Errors = {};
        for (const [field, detail] of Object.entries(error.fields)) {
          if (typeof detail !== 'string') continue;
          const key = field.replace(/\[\d+\]$/, '');
          if (key in emptyForm || key === 'idCategorias') fields[key as keyof Errors] = detail;
        }
        setErrors(fields);
        setMessage(Object.keys(fields).length ? 'Confira os campos destacados.' : error.message);
      } else if (error instanceof NetworkError) {
        setMessage(
          'Não foi possível confirmar o cadastro. Tente entrar antes de enviar novamente.',
        );
      } else {
        setMessage(error instanceof Error ? error.message : 'Não foi possível enviar o cadastro.');
      }
    } finally {
      submitting.current = false;
      if (active.current) setSaving(false);
    }
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        title="Cadastro de prestador"
        onBack={saving ? undefined : () => router.dismissTo('/cadastro')}
      />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets>
        <SignUpProgress label="Cadastro profissional" step={2} totalSteps={2} />
        <View style={styles.section}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Seus dados
          </Text>
          <TextField
            label="Nome completo"
            value={form.nome}
            onChangeText={(value) => update('nome', value)}
            error={errors.nome}
            maxLength={150}
            autoComplete="name"
            editable={!saving}
          />
          <TextField
            label="CPF"
            value={form.cpf}
            onChangeText={(value) => update('cpf', formatCpf(value))}
            error={errors.cpf}
            maxLength={14}
            keyboardType="number-pad"
            editable={!saving}
          />
          <TextField
            label="Telefone"
            value={form.telefone}
            onChangeText={(value) => update('telefone', formatPhone(value))}
            error={errors.telefone}
            maxLength={15}
            keyboardType="phone-pad"
            autoComplete="tel"
            editable={!saving}
          />
          <TextField
            label="E-mail"
            value={form.email}
            onChangeText={(value) => update('email', value)}
            error={errors.email}
            maxLength={150}
            autoComplete="email"
            keyboardType="email-address"
            autoCapitalize="none"
            editable={!saving}
          />
          <TextField
            label="Senha"
            value={form.senha}
            onChangeText={(value) => update('senha', value)}
            error={errors.senha}
            maxLength={PASSWORD_MAX_LENGTH}
            autoComplete="new-password"
            secureTextEntry
            editable={!saving}
          />
        </View>
        <View style={styles.section}>
          <Text variant="titleLarge" accessibilityRole="header">
            Serviços oferecidos
          </Text>
          {catalog.state === 'loading' ? (
            <ActivityIndicator accessibilityLabel="Carregando categorias" />
          ) : catalog.state === 'error' || !catalog.categories.length ? (
            <>
              <AlertBanner>
                {catalog.state === 'error'
                  ? catalog.message
                  : 'Não há categorias disponíveis no momento.'}
              </AlertBanner>
              <Button mode="outlined" onPress={retryCatalog}>
                Atualizar categorias
              </Button>
            </>
          ) : (
            catalog.categories.map(({ idCategoria, nome }) => (
              <Checkbox
                key={idCategoria}
                label={nome}
                checked={categoryIds.includes(idCategoria)}
                onChange={(checked) => toggleCategory(idCategoria, checked)}
                disabled={saving}
              />
            ))
          )}
          {errors.idCategorias && (
            <Text style={{ color: colors.error }}>{errors.idCategorias}</Text>
          )}
        </View>
        <View style={styles.section}>
          <Text variant="titleLarge" accessibilityRole="header">
            Área de atendimento
          </Text>
          <View style={styles.cityRow}>
            <View style={styles.city}>
              <TextField
                label="Cidade"
                value={form.cidade}
                onChangeText={(value) => update('cidade', value)}
                error={errors.cidade}
                maxLength={100}
                autoCapitalize="words"
                editable={!saving}
              />
            </View>
            <View style={styles.uf}>
              <TextField
                label="UF"
                value={form.uf}
                onChangeText={(value) => update('uf', value.toUpperCase())}
                error={errors.uf}
                maxLength={2}
                autoCapitalize="characters"
                editable={!saving}
              />
            </View>
          </View>
          <TextField
            label="Bairros atendidos (opcional)"
            placeholder="Ex.: Centro, Bela Vista"
            value={form.bairros}
            onChangeText={(value) => update('bairros', value)}
            error={errors.bairros}
            maxLength={3059}
            editable={!saving}
          />
          <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
            Separe os bairros por vírgula ou deixe em branco se atende toda a cidade.
          </Text>
        </View>
        <View style={styles.section}>
          <Checkbox
            label="Adicionar dados de empresa"
            checked={hasCompany}
            onChange={toggleCompany}
            disabled={saving}
          />
          {hasCompany && (
            <>
              <TextField
                label="Razão social"
                value={form['empresa.razaoSocial']}
                onChangeText={(value) => update('empresa.razaoSocial', value)}
                error={errors['empresa.razaoSocial']}
                maxLength={200}
                editable={!saving}
              />
              <TextField
                label="Nome fantasia (opcional)"
                value={form['empresa.nomeFantasia']}
                onChangeText={(value) => update('empresa.nomeFantasia', value)}
                error={errors['empresa.nomeFantasia']}
                maxLength={200}
                editable={!saving}
              />
              <TextField
                label="CNPJ"
                value={form['empresa.cnpj']}
                onChangeText={(value) => update('empresa.cnpj', formatCnpj(value))}
                error={errors['empresa.cnpj']}
                maxLength={18}
                autoCapitalize="characters"
                editable={!saving}
              />
            </>
          )}
        </View>
        <Checkbox
          label="Li e aceito os Termos de Uso e a Política de Privacidade"
          checked={acceptedTerms}
          onChange={(checked) => {
            setAcceptedTerms(checked);
            setErrors((previous) => ({ ...previous, termos: undefined }));
          }}
          error={errors.termos}
          disabled={saving}
        />
        {message && <AlertBanner>{message}</AlertBanner>}
        <Button
          mode="contained"
          contentStyle={styles.button}
          onPress={submit}
          loading={saving}
          disabled={saving || catalog.state !== 'loaded' || catalog.categories.length === 0}>
          {saving ? 'Enviando cadastro...' : 'Enviar cadastro'}
        </Button>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
    gap: 24,
    padding: 20,
    paddingBottom: 32,
  },
  section: { gap: 14 },
  cityRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 12 },
  city: { flex: 1 },
  uf: { width: 88 },
  button: { height: 52 },
});
