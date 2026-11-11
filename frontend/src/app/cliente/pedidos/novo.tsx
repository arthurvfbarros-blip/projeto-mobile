import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Dialog, Portal, RadioButton, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AlertBanner } from '@/components/alert-banner';
import { OrderSummary } from '@/components/order-summary';
import { InfoBanner } from '@/components/info-banner';
import { ScreenHeader } from '@/components/screen-header';
import { SignUpProgress } from '@/components/signup-progress';
import { TextField } from '@/components/text-field';
import { SubmitButton } from '@/components/submit-button';
import { useAppTheme } from '@/constants/theme';
import { useOrderDraft } from '@/providers/order-draft-provider';
import { useSession } from '@/providers/session-provider';
import { listAddresses, type Endereco } from '@/services/addresses';
import { ApiError, NetworkError } from '@/services/api';
import { listCategories, type Categoria } from '@/services/categories';
import { createOrder } from '@/services/orders';
import { formatCep, formatDate } from '@/utils/masks';
import {
  dateToIso,
  validateOrder,
  type OrderDraft,
  type OrderErrors,
} from '@/utils/order-validation';

export default function NewOrderScreen() {
  const { colors } = useAppTheme();
  const { session, expireSession } = useSession();
  const { draft, step, updateDraft, setStep, resetDraft } = useOrderDraft();
  const [categories, setCategories] = useState<Categoria[]>([]);
  const [addresses, setAddresses] = useState<Endereco[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [errors, setErrors] = useState<OrderErrors>({});
  const [choosingCategory, setChoosingCategory] = useState(false);
  const [saving, setSaving] = useState(false);
  const submitting = useRef(false);
  const active = useRef(false);
  const generation = useRef(0);
  const token = session?.accessToken;
  const category = categories.find((item) => item.idCategoria === draft.idCategoria);
  const address = addresses.find((item) => item.idEndereco === draft.idEndereco);

  const load = useCallback(async () => {
    if (!token) return;
    const request = ++generation.current;
    setLoading(true);
    setLoadError(null);
    try {
      const [availableCategories, ownAddresses] = await Promise.all([
        listCategories(token),
        listAddresses(token),
      ]);
      if (request !== generation.current) return;
      setCategories(availableCategories);
      setAddresses(ownAddresses);
    } catch (failure) {
      if (request !== generation.current) return;
      if (failure instanceof ApiError && failure.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else
        setLoadError(
          failure instanceof Error
            ? failure.message
            : 'Não foi possível carregar os dados do pedido.',
        );
    } finally {
      if (request === generation.current) setLoading(false);
    }
  }, [expireSession, token]);

  useFocusEffect(
    useCallback(() => {
      active.current = true;
      void load();
      return () => {
        active.current = false;
        generation.current++;
      };
    }, [load]),
  );

  function update<K extends keyof OrderDraft>(field: K, value: OrderDraft[K]) {
    updateDraft({ [field]: value });
    setErrors((current) => ({ ...current, [field]: undefined }));
    setMessage(null);
  }

  function validate(): OrderErrors {
    const validation = validateOrder(draft);
    if (!category) validation.idCategoria = 'Selecione uma categoria disponível';
    if (!address) validation.idEndereco = 'Selecione um endereço cadastrado na sua conta';
    return validation;
  }

  function next() {
    const validation = validate();
    const fields: (keyof OrderDraft)[] =
      step === 1 ? ['idCategoria', 'titulo', 'descricao'] : ['idEndereco', 'dataDesejada'];
    const visible: OrderErrors = {};
    for (const field of fields) if (validation[field]) visible[field] = validation[field];
    setErrors(visible);
    setMessage(null);
    if (Object.values(visible).some(Boolean)) {
      setMessage('Confira os campos destacados para continuar.');
      return;
    }
    setStep(step + 1);
  }

  async function submit() {
    if (!token || submitting.current) return;
    const validation = validate();
    setErrors(validation);
    setMessage(null);
    if (Object.values(validation).some(Boolean)) {
      setStep(validation.idCategoria || validation.titulo || validation.descricao ? 1 : 2);
      return;
    }
    submitting.current = true;
    setSaving(true);
    try {
      const created = await createOrder(token, {
        idCategoria: draft.idCategoria!,
        idEndereco: draft.idEndereco!,
        titulo: draft.titulo.trim(),
        descricao: draft.descricao.trim(),
        dataDesejada: dateToIso(draft.dataDesejada)!,
      });
      if (!active.current) return;
      resetDraft();
      router.dismissTo({
        pathname: '/cliente/pedidos/[id]',
        params: { id: created.idPedido.toString(), pedidoCriado: '1' },
      });
    } catch (error) {
      if (!active.current) return;
      if (error instanceof ApiError && error.status === 401) {
        await expireSession(token).catch(() => {
          console.error('Não foi possível limpar a sessão expirada.');
        });
      } else if (error instanceof ApiError) {
        const fields: OrderErrors = {};
        for (const field of Object.keys(draft) as (keyof OrderDraft)[]) {
          if (typeof error.fields[field] === 'string') fields[field] = error.fields[field];
        }
        setErrors(fields);
        if (fields.idCategoria || fields.titulo || fields.descricao) setStep(1);
        else if (fields.idEndereco || fields.dataDesejada) setStep(2);
        else setMessage(error.message);
      } else if (error instanceof NetworkError) {
        setMessage(
          'Não foi possível confirmar o envio. Confira Meus pedidos antes de tentar novamente.',
        );
      } else
        setMessage(
          error instanceof Error
            ? error.message
            : 'Não foi possível enviar o pedido. Tente novamente.',
        );
    } finally {
      submitting.current = false;
      if (active.current) setSaving(false);
    }
  }

  function back() {
    setMessage(null);
    setErrors({});
    if (step > 1) setStep(step - 1);
    else router.dismissTo('/cliente/pedidos');
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader variant="app" title="Novo pedido" onBack={saving ? undefined : back} />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets>
        <SignUpProgress
          variant="segmented"
          label={
            step === 1 ? 'Dados do serviço' : step === 2 ? 'Endereço e data' : 'Revisão do pedido'
          }
          step={step}
          totalSteps={3}
        />
        {loading ? (
          <View style={styles.state}>
            <ActivityIndicator accessibilityLabel="Carregando dados do pedido" />
            <Text>Carregando dados do pedido...</Text>
          </View>
        ) : loadError ? (
          <>
            <AlertBanner>{loadError}</AlertBanner>
            <Button
              mode="outlined"
              onPress={() => {
                void load();
              }}>
              Tentar novamente
            </Button>
          </>
        ) : (
          <>
            {step === 1 ? (
              <View style={styles.fields}>
                {categories.length === 0 ? (
                  <>
                    <AlertBanner>Não há categorias disponíveis no momento.</AlertBanner>
                    <Button
                      mode="outlined"
                      onPress={() => {
                        void load();
                      }}>
                      Atualizar categorias
                    </Button>
                  </>
                ) : (
                  <View style={styles.field}>
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel="Selecionar categoria"
                      disabled={saving}
                      style={[
                        styles.categoryField,
                        {
                          backgroundColor: errors.idCategoria
                            ? colors.errorContainer
                            : colors.surfaceVariant,
                          borderBottomColor: errors.idCategoria
                            ? colors.error
                            : colors.onSurfaceVariant,
                        },
                      ]}
                      onPress={() => setChoosingCategory(true)}>
                      <Text
                        variant="bodySmall"
                        style={{
                          color: errors.idCategoria ? colors.error : colors.onSurfaceVariant,
                        }}>
                        Categoria do serviço
                      </Text>
                      <Text variant="bodyLarge">{category?.nome ?? 'Selecione uma categoria'}</Text>
                    </Pressable>
                    {errors.idCategoria && (
                      <Text style={{ color: colors.error }}>{errors.idCategoria}</Text>
                    )}
                  </View>
                )}
                <TextField
                  label="Título do pedido"
                  placeholder="Ex.: instalar chuveiro elétrico"
                  value={draft.titulo}
                  onChangeText={(value) => update('titulo', value)}
                  maxLength={150}
                  editable={!saving}
                  error={errors.titulo}
                />
                <TextField
                  label="Descrição"
                  placeholder="Detalhe o serviço e o que você precisa"
                  value={draft.descricao}
                  onChangeText={(value) => update('descricao', value)}
                  maxLength={5000}
                  multiline
                  editable={!saving}
                  error={errors.descricao}
                />
                <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                  Descreva o serviço com pelo menos 20 caracteres.
                </Text>
              </View>
            ) : step === 2 ? (
              <View style={styles.fields}>
                <Text variant="headlineSmall" accessibilityRole="header">
                  Onde e quando?
                </Text>
                {addresses.length === 0 ? (
                  <View style={[styles.empty, { borderColor: colors.outlineVariant }]}>
                    <Text variant="titleMedium">Cadastre um endereço para continuar</Text>
                    <Text variant="bodyMedium">
                      Seu pedido será mantido enquanto você cadastra o endereço.
                    </Text>
                  </View>
                ) : (
                  addresses.map((item) => (
                    <Pressable
                      key={item.idEndereco}
                      accessibilityRole="radio"
                      accessibilityLabel={`${item.logradouro}, ${item.numero}`}
                      accessibilityState={{ checked: item.idEndereco === draft.idEndereco }}
                      aria-checked={item.idEndereco === draft.idEndereco}
                      disabled={saving}
                      onPress={() => update('idEndereco', item.idEndereco)}
                      style={[
                        styles.address,
                        {
                          borderColor:
                            item.idEndereco === draft.idEndereco
                              ? colors.primary
                              : colors.outlineVariant,
                          backgroundColor:
                            item.idEndereco === draft.idEndereco
                              ? colors.primaryContainer
                              : colors.surfaceContainerLowest,
                        },
                      ]}>
                      <Text variant="titleMedium">
                        {item.logradouro}, {item.numero}
                      </Text>
                      {item.complemento ? (
                        <Text variant="bodyMedium">{item.complemento}</Text>
                      ) : null}
                      <Text variant="bodyMedium">
                        {item.bairro} · {item.cidade} / {item.uf}
                      </Text>
                      <Text variant="bodySmall">CEP {formatCep(item.cep)}</Text>
                    </Pressable>
                  ))
                )}
                {errors.idEndereco && (
                  <Text style={{ color: colors.error }}>{errors.idEndereco}</Text>
                )}
                <Button
                  mode="outlined"
                  disabled={saving}
                  onPress={() =>
                    router.push({
                      pathname: '/cliente/enderecos/novo',
                      params: { retorno: 'pedido' },
                    })
                  }>
                  Cadastrar novo endereço
                </Button>
                <TextField
                  label="Data desejada"
                  placeholder="DD/MM/AAAA"
                  value={draft.dataDesejada}
                  onChangeText={(value) => update('dataDesejada', formatDate(value))}
                  keyboardType="number-pad"
                  maxLength={10}
                  editable={!saving}
                  error={errors.dataDesejada}
                />
                <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                  Essa data é uma preferência. O horário será definido no agendamento.
                </Text>
              </View>
            ) : (
              <>
                <Text variant="headlineSmall" accessibilityRole="header">
                  Revise antes de enviar
                </Text>
                <View style={[styles.review, { borderColor: colors.outlineVariant }]}>
                  <OrderSummary
                    titulo={draft.titulo.trim()}
                    descricao={draft.descricao.trim()}
                    categoria={category?.nome ?? 'Categoria não disponível'}
                    dataDesejada={dateToIso(draft.dataDesejada)}
                    endereco={address ?? null}
                  />
                </View>
                <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                  Seu pedido só será publicado ao tocar em Enviar pedido.
                </Text>
              </>
            )}
            {step === 1 && !draft.descricao.trim() && !message && (
              <InfoBanner icon="info">
                Quanto mais detalhes, mais precisos serão os orçamentos.
              </InfoBanner>
            )}
            {message && <AlertBanner>{message}</AlertBanner>}
            <View style={styles.actions}>
              {step < 3 ? (
                <Button
                  mode="contained"
                  contentStyle={styles.button}
                  onPress={next}
                  disabled={categories.length === 0 || (step === 2 && addresses.length === 0)}>
                  Continuar
                </Button>
              ) : (
                <SubmitButton
                  contentStyle={styles.button}
                  loading={saving}
                  disabled={saving}
                  onPress={submit}>
                  {saving ? 'Enviando pedido...' : 'Enviar pedido'}
                </SubmitButton>
              )}
              {step > 1 && (
                <Button mode="outlined" disabled={saving} onPress={back}>
                  Voltar etapa
                </Button>
              )}
            </View>
          </>
        )}
        <Button
          mode="text"
          disabled={saving}
          onPress={() => {
            resetDraft();
            router.dismissTo('/cliente/pedidos');
          }}>
          Descartar rascunho
        </Button>
      </ScrollView>
      <Portal>
        <Dialog visible={choosingCategory} onDismiss={() => setChoosingCategory(false)}>
          <Dialog.Title>Categoria do serviço</Dialog.Title>
          <Dialog.ScrollArea>
            <ScrollView style={styles.categoryList}>
              <RadioButton.Group
                value={draft.idCategoria?.toString() ?? ''}
                onValueChange={(value) => {
                  update('idCategoria', Number(value));
                  setChoosingCategory(false);
                }}>
                {categories.map((item) => (
                  <RadioButton.Item
                    key={item.idCategoria}
                    label={item.nome}
                    value={item.idCategoria.toString()}
                  />
                ))}
              </RadioButton.Group>
            </ScrollView>
          </Dialog.ScrollArea>
          <Dialog.Actions>
            <Button onPress={() => setChoosingCategory(false)}>Fechar</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: {
    padding: 16,
    paddingTop: 4,
    paddingBottom: 32,
    gap: 16,
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  fields: { gap: 16 },
  field: { gap: 8 },
  actions: { gap: 12 },
  button: { height: 40 },
  categoryField: {
    minHeight: 58,
    paddingHorizontal: 16,
    paddingVertical: 8,
    gap: 2,
    borderTopLeftRadius: 4,
    borderTopRightRadius: 4,
    borderBottomWidth: 1,
  },
  state: { paddingVertical: 32, gap: 16, alignItems: 'center' },
  empty: { padding: 16, gap: 8, borderWidth: 1, borderRadius: 12 },
  address: { padding: 16, gap: 6, borderWidth: 1, borderRadius: 12 },
  review: { padding: 16, borderWidth: 1, borderRadius: 12 },
  categoryList: { maxHeight: 320 },
});
