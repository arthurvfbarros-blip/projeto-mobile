import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { randomUUID } from 'expo-crypto';
import { useCallback, useRef, useState, type ComponentRef } from 'react';
import {
  FlatList,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
} from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AlertBanner } from '@/components/alert-banner';
import { ScreenHeader } from '@/components/screen-header';
import { ScreenState } from '@/components/screen-state';
import { SubmitButton } from '@/components/submit-button';
import { useAppTheme } from '@/constants/theme';
import { useSession } from '@/providers/session-provider';
import { ApiError } from '@/services/api';
import { type AgendaProfile } from '@/services/appointments';
import {
  connectChat,
  getChat,
  getMessages,
  sendMessage,
  type ChatContext,
  type Mensagem,
} from '@/services/chat';
import { readNotification, type Notificacao } from '@/services/notifications';
import { formatMoney, validRequestId } from '@/utils/quote-validation';

type PendingMessage = { id: string; text: string };
export function ChatScreen({ profile }: { profile: AgendaProfile }) {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [reload, setReload] = useState(0);
  return (
    <Conversation
      key={`${id}:${reload}`}
      id={id}
      profile={profile}
      onRetry={() => setReload((current) => current + 1)}
    />
  );
}
function Conversation({
  id,
  profile,
  onRetry,
}: {
  id: string;
  profile: AgendaProfile;
  onRetry: () => void;
}) {
  const { colors, fonts } = useAppTheme(),
    { session, expireSession } = useSession();
  const token = session?.accessToken;
  const appointment = validRequestId(id);
  const [context, setContext] = useState<ChatContext | null>(null),
    [messages, setMessages] = useState<Mensagem[]>([]);
  const [loading, setLoading] = useState(true),
    [blocked, setBlocked] = useState(false),
    [loadError, setLoadError] = useState<string | null>(null);
  const [syncWarning, setSyncWarning] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null),
    [text, setText] = useState(''),
    [sending, setSending] = useState(false);
  const [connected, setConnected] = useState(false),
    [contacts, setContacts] = useState(false),
    [older, setOlder] = useState<number | null>(null),
    [loadingOlder, setLoadingOlder] = useState(false);
  const [notification, setNotification] = useState<Notificacao | null>(null);
  const generation = useRef(0),
    latest = useRef(0),
    pending = useRef<PendingMessage | null>(null),
    submitting = useRef(false),
    atBottom = useRef(true);
  const list = useRef<ComponentRef<typeof FlatList<Mensagem>>>(null);
  const socket = useRef<ReturnType<typeof connectChat> | null>(null);
  const merge = useCallback((incoming: Mensagem[], scroll = false) => {
    if (!incoming.length) return;
    latest.current = Math.max(latest.current, ...incoming.map((m) => m.idMensagem));
    setMessages((current) =>
      [...new Map([...current, ...incoming].map((m) => [m.idMensagem, m])).values()].sort(
        (a, b) => b.idMensagem - a.idMensagem,
      ),
    );
    if (scroll || atBottom.current)
      requestAnimationFrame(() => list.current?.scrollToOffset({ offset: 0, animated: true }));
  }, []);
  const handleFailure = useCallback(
    async (failure: unknown) => {
      if (failure instanceof ApiError && failure.status === 401 && token) {
        await expireSession(token).catch(() =>
          console.error('Não foi possível limpar a sessão expirada.'),
        );
        return true;
      }
      if (failure instanceof ApiError && (failure.status === 403 || failure.status === 404)) {
        setContext(null);
        setMessages([]);
        setBlocked(true);
        setLoadError(
          failure.status === 404
            ? 'Esta conversa não está disponível na sua conta.'
            : failure.message,
        );
        void socket.current?.deactivate();
        return true;
      }
      return false;
    },
    [expireSession, token],
  );

  const start = useCallback(() => {
    if (!token || appointment === null) {
      setLoading(false);
      setLoadError('Conversa não encontrada.');
      return () => {};
    }
    const request = ++generation.current;
    let client: ReturnType<typeof connectChat> | null = null;
    let interval: ReturnType<typeof setInterval> | undefined;
    let synchronizing = false;
    const active = () => request === generation.current;
    async function sync() {
      if (!active() || synchronizing) return;
      synchronizing = true;
      try {
        let cursor = latest.current;
        do {
          const page = await getMessages(token!, appointment!, { depoisDe: cursor });
          if (!active()) return;
          merge(page.itens);
          if (!page.temMais || page.proximoCursor === null) break;
          cursor = page.proximoCursor;
        } while (active());
        if (active()) setSyncWarning(null);
      } catch (failure) {
        if (active() && !(await handleFailure(failure)))
          setSyncWarning('Não foi possível atualizar as mensagens. Tente novamente.');
      } finally {
        synchronizing = false;
      }
    }
    async function load() {
      setLoading(true);
      setLoadError(null);
      setBlocked(false);
      setError(null);
      setConnected(false);
      try {
        const [c, page] = await Promise.all([
          getChat(token!, appointment!),
          getMessages(token!, appointment!),
        ]);
        if (!active()) return;
        setContext(c);
        setMessages(page.itens.slice().reverse());
        latest.current = page.itens.at(-1)?.idMensagem ?? 0;
        setOlder(page.proximoCursor);
        atBottom.current = true;
        client = connectChat(token!, appointment!, {
          onNotification:
            profile === 'prestador'
              ? (notification) => {
                  if (active() && notification.idAgendamento === appointment) {
                    setNotification(notification);
                    void getChat(token!, appointment!)
                      .then((result) => {
                        if (active()) setContext(result);
                      })
                      .catch((failure) => {
                        if (active()) void handleFailure(failure);
                      });
                  }
                }
              : undefined,
          onConnected: () => {
            if (active()) {
              setConnected(true);
              void sync();
            }
          },
          onDisconnected: () => {
            if (active()) setConnected(false);
          },
          onMessage: (m) => {
            if (active()) merge([m]);
          },
          onError: () => {
            if (active()) setConnected(false);
          },
        });
        socket.current = client;
        interval = setInterval(() => {
          void sync();
        }, 15000);
      } catch (failure) {
        if (active() && !(await handleFailure(failure)))
          setLoadError(
            failure instanceof Error ? failure.message : 'Não foi possível carregar a conversa.',
          );
      } finally {
        if (active()) setLoading(false);
      }
    }
    void load();
    return () => {
      generation.current++;
      if (interval) clearInterval(interval);
      void client?.deactivate();
      socket.current = null;
    };
  }, [appointment, handleFailure, merge, profile, token]);
  useFocusEffect(start);

  async function loadOlder() {
    if (!token || appointment === null || older === null || loadingOlder) return;
    const request = generation.current;
    setLoadingOlder(true);
    setError(null);
    try {
      const page = await getMessages(token, appointment, { antesDe: older });
      if (request === generation.current) {
        merge(page.itens);
        setOlder(page.proximoCursor);
      }
    } catch (failure) {
      if (request === generation.current && !(await handleFailure(failure)))
        setError('Não foi possível carregar as mensagens anteriores.');
    } finally {
      if (request === generation.current) setLoadingOlder(false);
    }
  }
  async function send() {
    if (!token || appointment === null || !context || submitting.current) return;
    const value = text.trim();
    if (!value) {
      setError('Digite uma mensagem.');
      return;
    }
    if (value.length > 2000) {
      setError('A mensagem deve ter no máximo 2000 caracteres.');
      return;
    }
    const request = generation.current;
    if (pending.current?.text !== value) pending.current = { id: randomUUID(), text: value };
    const attempt = pending.current;
    submitting.current = true;
    setSending(true);
    setError(null);
    try {
      const message = await sendMessage(token, appointment, attempt.id, attempt.text);
      if (request === generation.current) {
        merge([message], true);
        setText('');
        pending.current = null;
      }
    } catch (failure) {
      if (request === generation.current && !(await handleFailure(failure)))
        setError(
          failure instanceof Error ? failure.message : 'Não foi possível enviar a mensagem.',
        );
    } finally {
      submitting.current = false;
      if (request === generation.current) setSending(false);
    }
  }
  async function openNotification() {
    if (!token || !notification) return;
    try {
      await readNotification(token, notification.idNotificacao);
      router.push(`/prestador/agendamentos/${notification.idAgendamento}`);
    } catch (failure) {
      if (!(await handleFailure(failure)))
        setError(
          failure instanceof Error ? failure.message : 'Não foi possível abrir a contratação.',
        );
    }
  }
  const contact = context ? (profile === 'cliente' ? context.prestador : context.cliente) : null;
  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: colors.surfaceContainerLowest }]}>
      <KeyboardAvoidingView
        style={styles.screen}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScreenHeader
          variant="app"
          title={contact?.nome ?? 'Conversa'}
          subtitle={context?.tituloPedido}
          onBack={() =>
            router.dismissTo(
              profile === 'cliente'
                ? `/cliente/agendamentos/${id}`
                : `/prestador/agendamentos/${id}`,
            )
          }
        />
        {loading ? (
          <View style={styles.loading}>
            <ActivityIndicator accessibilityLabel="Carregando conversa" />
          </View>
        ) : loadError ? (
          <ScrollView contentContainerStyle={styles.state}>
            <ScreenState
              kind={blocked ? 'restricted' : 'error'}
              title="Conversa indisponível"
              message={loadError}
            />
            <Button mode="outlined" onPress={onRetry}>
              Tentar novamente
            </Button>
          </ScrollView>
        ) : (
          context && (
            <>
              <View style={[styles.toolbar, { borderBottomColor: colors.outlineVariant }]}>
                <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
                  {connected ? 'Conectado' : 'Reconectando…'}
                </Text>
                <Button compact onPress={() => setContacts(!contacts)}>
                  {contacts ? 'Ocultar contatos' : 'Ver contatos'}
                </Button>
              </View>
              {context.contratacao && (
                <View style={{ padding: 12, backgroundColor: colors.successContainer }}>
                  <Text variant="labelLarge" style={{ color: colors.onSuccessContainer }}>
                    Serviço contratado · {formatMoney(context.contratacao.valor)}
                  </Text>
                </View>
              )}
              {contacts && (
                <View style={[styles.contacts, { backgroundColor: colors.primaryContainer }]}>
                  <Text variant="labelLarge">Contato de {contact?.nome}</Text>
                  <Text variant="bodyMedium" selectable>
                    Telefone: {contact?.telefone || 'Não informado'}
                  </Text>
                  <Text variant="bodyMedium" selectable>
                    {context.endereco.logradouro}, {context.endereco.numero}
                    {context.endereco.complemento ? ' · ' + context.endereco.complemento : ''}
                  </Text>
                  <Text variant="bodyMedium">
                    {context.endereco.bairro} · {context.endereco.cidade} / {context.endereco.uf}
                  </Text>
                </View>
              )}
              <FlatList
                ref={list}
                inverted
                data={messages}
                keyExtractor={(m) => String(m.idMensagem)}
                style={styles.messages}
                contentContainerStyle={styles.messageContent}
                keyboardShouldPersistTaps="handled"
                onScroll={(event) => {
                  atBottom.current = event.nativeEvent.contentOffset.y < 80;
                }}
                scrollEventThrottle={100}
                ListEmptyComponent={
                  <View style={styles.empty}>
                    <Text variant="titleMedium">Comece a conversa</Text>
                    <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
                      Combine os detalhes do serviço com {contact?.nome}.
                    </Text>
                  </View>
                }
                ListFooterComponent={
                  older !== null ? (
                    <Button loading={loadingOlder} disabled={loadingOlder} onPress={loadOlder}>
                      Carregar mensagens anteriores
                    </Button>
                  ) : undefined
                }
                renderItem={({ item }) => {
                  const mine = item.idRemetente === context.idUsuarioAtual;
                  return (
                    <View
                      style={[
                        styles.bubble,
                        {
                          alignSelf: mine ? 'flex-end' : 'flex-start',
                          backgroundColor: mine ? colors.primaryContainer : colors.surfaceVariant,
                        },
                      ]}
                    >
                      <Text variant="bodyLarge" selectable>
                        {item.conteudo}
                      </Text>
                      <Text
                        variant="labelSmall"
                        style={[styles.time, { color: colors.onSurfaceVariant }]}
                      >
                        {new Intl.DateTimeFormat('pt-BR', {
                          day: '2-digit',
                          month: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit',
                          timeZone: 'America/Sao_Paulo',
                        }).format(new Date(item.criadoEm))}
                      </Text>
                    </View>
                  );
                }}
              />
              {(error || syncWarning) && (
                <View style={styles.error}>
                  <AlertBanner>{error ?? syncWarning!}</AlertBanner>
                </View>
              )}
              <View style={[styles.composer, { borderTopColor: colors.outlineVariant }]}>
                <TextInput
                  accessibilityLabel="Mensagem"
                  placeholder="Digite sua mensagem..."
                  placeholderTextColor={colors.onSurfaceVariant}
                  value={text}
                  onChangeText={setText}
                  multiline
                  maxLength={2000}
                  editable={!sending}
                  style={[
                    styles.input,
                    fonts.bodyLarge,
                    { color: colors.onSurface, backgroundColor: colors.surfaceVariant },
                  ]}
                />
                <SubmitButton loading={sending} disabled={sending || !text.trim()} onPress={send}>
                  {sending ? 'Enviando...' : 'Enviar'}
                </SubmitButton>
              </View>
            </>
          )
        )}
      </KeyboardAvoidingView>
      <Snackbar
        visible={notification !== null}
        onDismiss={() => setNotification(null)}
        action={{
          label: 'Ver',
          onPress: () => {
            void openNotification();
          },
        }}
      >
        Contratação confirmada pelo cliente
      </Snackbar>
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  state: { padding: 16, gap: 16 },
  toolbar: {
    paddingHorizontal: 16,
    paddingVertical: 4,
    borderBottomWidth: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  contacts: { padding: 16, gap: 8 },
  messages: { flex: 1 },
  messageContent: { padding: 16, gap: 12, flexGrow: 1 },
  empty: { gap: 8, paddingVertical: 24, transform: [{ scaleY: -1 }] },
  bubble: { maxWidth: '88%', padding: 12, gap: 6, borderRadius: 16 },
  time: { alignSelf: 'flex-end' },
  composer: {
    padding: 12,
    gap: 8,
    borderTopWidth: 1,
    flexDirection: 'row',
    alignItems: 'flex-end',
  },
  input: {
    flex: 1,
    minHeight: 44,
    maxHeight: 120,
    borderRadius: 16,
    paddingHorizontal: 14,
    paddingVertical: 10,
    ...Platform.select({ web: { outlineWidth: 0, outlineStyle: 'solid' as const } }),
  },
  error: { paddingHorizontal: 12, paddingBottom: 8 },
});
