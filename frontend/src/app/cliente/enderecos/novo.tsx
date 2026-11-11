import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { FormField } from '@/components/form-field';
import { ScreenHeader } from '@/components/screen-header';
import { SubmitButton } from '@/components/submit-button';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { useOrderDraft } from '@/providers/order-draft-provider';
import { createAddress } from '@/services/addresses';
import { ApiError, NetworkError } from '@/services/api';
import { validateAddress, type AddressErrors, type AddressForm } from '@/utils/address-validation';
import { formatCep, onlyDigits } from '@/utils/masks';

export default function NewAddressScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const { updateDraft } = useOrderDraft();
  const { retorno } = useLocalSearchParams<{ retorno?: string }>();
  const returnToOrder = retorno === 'pedido';
  const goBack = () =>
    router.dismissTo(returnToOrder ? '/cliente/pedidos/novo' : '/cliente/enderecos');
  const [form, setForm] = useState<AddressForm>({
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    bairro: '',
    cidade: '',
    uf: '',
  });
  const [errors, setErrors] = useState<AddressErrors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const submitting = useRef(false);
  const active = useRef(true);

  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
    };
  }, []);

  function update(field: keyof AddressForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  async function handleSubmit() {
    const token = session?.accessToken;
    if (!token || submitting.current) return;
    setMessage(null);
    const validation = validateAddress(form);
    setErrors(validation);
    if (Object.values(validation).some(Boolean)) return;

    submitting.current = true;
    setSaving(true);
    try {
      const created = await createAddress(token, {
        cep: onlyDigits(form.cep),
        logradouro: form.logradouro.trim(),
        numero: form.numero.trim(),
        complemento: form.complemento.trim() || null,
        bairro: form.bairro.trim(),
        cidade: form.cidade.trim(),
        uf: form.uf.trim().toUpperCase(),
      });
      if (!active.current) return;
      if (returnToOrder) {
        updateDraft({ idEndereco: created.idEndereco });
        router.dismissTo('/cliente/pedidos/novo');
        return;
      }
      router.dismissTo({ pathname: '/cliente/enderecos', params: { enderecoCriado: '1' } });
    } catch (error) {
      if (!active.current) return;
      if (error instanceof ApiError && error.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else if (error instanceof ApiError) {
        const fields: AddressErrors = {};
        for (const field of Object.keys(form) as (keyof AddressForm)[]) {
          if (typeof error.fields[field] === 'string') fields[field] = error.fields[field];
        }
        setErrors(fields);
        if (Object.keys(fields).length === 0) setMessage(error.message);
      } else if (error instanceof NetworkError) {
        setMessage(error.message);
      } else {
        setMessage(
          error instanceof Error
            ? error.message
            : 'Não foi possível cadastrar o endereço. Tente novamente.',
        );
      }
    } finally {
      submitting.current = false;
      if (active.current) setSaving(false);
    }
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader variant="app" title="Novo endereço" onBack={saving ? undefined : goBack} />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets>
        <View style={styles.presentation}>
          <Text variant="headlineSmall" accessibilityRole="header">
            Onde você precisa de atendimento?
          </Text>
          <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
            Preencha os dados do endereço.
          </Text>
        </View>
        <View style={styles.fields}>
          <FormField
            label="CEP"
            icon={require('@/assets/icons/field-map-pin.svg')}
            placeholder="00000-000"
            value={form.cep}
            onChangeText={(value) => update('cep', formatCep(value))}
            error={errors.cep}
            editable={!saving}
            maxLength={9}
            keyboardType="number-pad"
            autoComplete="postal-code"
            textContentType="postalCode"
          />
          <FormField
            label="Logradouro"
            icon={require('@/assets/icons/field-map-pin.svg')}
            placeholder="Rua, avenida, travessa..."
            value={form.logradouro}
            onChangeText={(value) => update('logradouro', value)}
            error={errors.logradouro}
            editable={!saving}
            maxLength={150}
            autoCapitalize="words"
            autoComplete="address-line1"
            textContentType="streetAddressLine1"
          />
          <FormField
            label="Número"
            icon={require('@/assets/icons/field-building.svg')}
            placeholder="Número ou S/N"
            value={form.numero}
            onChangeText={(value) => update('numero', value)}
            error={errors.numero}
            editable={!saving}
            maxLength={20}
            autoCapitalize="characters"
          />
          <FormField
            label="Complemento (opcional)"
            icon={require('@/assets/icons/field-building.svg')}
            placeholder="Apartamento, bloco, referência..."
            value={form.complemento}
            onChangeText={(value) => update('complemento', value)}
            error={errors.complemento}
            editable={!saving}
            maxLength={100}
            autoComplete="address-line2"
            textContentType="streetAddressLine2"
          />
          <FormField
            label="Bairro"
            icon={require('@/assets/icons/field-map-pin.svg')}
            placeholder="Seu bairro"
            value={form.bairro}
            onChangeText={(value) => update('bairro', value)}
            error={errors.bairro}
            editable={!saving}
            maxLength={100}
            autoCapitalize="words"
          />
          <FormField
            label="Cidade"
            icon={require('@/assets/icons/field-building.svg')}
            placeholder="Sua cidade"
            value={form.cidade}
            onChangeText={(value) => update('cidade', value)}
            error={errors.cidade}
            editable={!saving}
            maxLength={100}
            autoCapitalize="words"
            textContentType="addressCity"
          />
          <FormField
            label="UF"
            icon={require('@/assets/icons/field-map-pin.svg')}
            placeholder="Ex.: SP"
            value={form.uf}
            onChangeText={(value) => update('uf', value.toUpperCase())}
            error={errors.uf}
            editable={!saving}
            maxLength={2}
            autoCapitalize="characters"
            autoCorrect={false}
            textContentType="addressState"
          />
        </View>
        {message && <AlertBanner>{message}</AlertBanner>}
        <View style={styles.actions}>
          <SubmitButton
            contentStyle={styles.button}
            onPress={handleSubmit}
            loading={saving}
            disabled={saving}>
            {saving ? 'Salvando endereço...' : 'Salvar endereço'}
          </SubmitButton>
          <Button mode="text" onPress={goBack} disabled={saving}>
            Cancelar
          </Button>
        </View>
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
  fields: { gap: 16 },
  actions: { gap: 12 },
  button: { height: 52 },
});
