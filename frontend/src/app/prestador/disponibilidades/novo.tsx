import { router } from 'expo-router';
import { useRef, useState } from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import { Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { SubmitButton } from '@/components/submit-button';
import { TextField } from '@/components/text-field';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { createSlot } from '@/services/appointments';
import { maskTime } from '@/utils/appointment-format';
import { dateToIso, todayInBrazil } from '@/utils/order-validation';

export default function NewAvailability() {
  const { colors } = useAppTheme(),
    { session, expireSession } = useSession();
  const [data, setData] = useState(''),
    [hora, setHora] = useState(''),
    [errors, setErrors] = useState<Record<string, string>>({}),
    [message, setMessage] = useState<string | null>(null),
    [saving, setSaving] = useState(false);
  const submitting = useRef(false);
  async function save() {
    if (!session || submitting.current) return;
    const iso = dateToIso(data),
      validation: Record<string, string> = {};
    if (!iso || iso < todayInBrazil())
      validation.data = 'Informe uma data futura no formato DD/MM/AAAA';
    if (!/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(hora))
      validation.hora = 'Informe um horário válido no formato HH:mm';
    setErrors(validation);
    setMessage(null);
    if (Object.keys(validation).length) return;
    submitting.current = true;
    setSaving(true);
    try {
      await createSlot(session.accessToken, iso!, hora);
      router.dismissTo('/prestador/disponibilidades');
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401)
        await expireSession(session.accessToken).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
      else {
        if (failure instanceof ApiError) setErrors(failure.fields);
        setMessage(
          failure instanceof Error ? failure.message : 'Não foi possível cadastrar o horário.',
        );
      }
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  }
  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader
        variant="app"
        title="Adicionar horário"
        onBack={() => {
          if (!submitting.current) router.dismissTo('/prestador/disponibilidades');
        }}
      />
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text variant="bodyMedium">
          Escolha uma data e um horário futuros em que pode iniciar o atendimento. Horário de
          Brasília.
        </Text>
        <TextField
          label="Data disponível"
          placeholder="DD/MM/AAAA"
          value={data}
          maxLength={10}
          keyboardType="number-pad"
          editable={!saving}
          error={errors.data}
          onChangeText={(value) => {
            const digits = value.replace(/\D/g, '').slice(0, 8);
            setData(
              digits.length > 4
                ? digits.slice(0, 2) + '/' + digits.slice(2, 4) + '/' + digits.slice(4)
                : digits.length > 2
                  ? digits.slice(0, 2) + '/' + digits.slice(2)
                  : digits,
            );
            setErrors((current) => ({ ...current, data: '' }));
          }}
        />
        <TextField
          label="Horário disponível"
          placeholder="HH:mm"
          value={hora}
          maxLength={5}
          keyboardType="number-pad"
          editable={!saving}
          error={errors.hora}
          onChangeText={(value) => {
            setHora(maskTime(value));
            setErrors((current) => ({ ...current, hora: '' }));
          }}
        />
        {message && <AlertBanner>{message}</AlertBanner>}
        <SubmitButton loading={saving} disabled={saving} onPress={save}>
          {saving ? 'Salvando...' : 'Salvar horário'}
        </SubmitButton>
      </ScrollView>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, gap: 16, paddingBottom: 32 },
});
