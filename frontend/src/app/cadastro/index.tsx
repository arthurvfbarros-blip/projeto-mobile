import { Image, type ImageProps } from 'expo-image';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ArrowRightIcon } from '@/components/arrow-right-icon';
import { ScreenHeader } from '@/components/screen-header';
import { SignUpProgress } from '@/components/signup-progress';
import { useAppTheme } from '@/constants/theme';

const PROFILES = {
  usuario: {
    title: 'Sou usuário',
    description: 'Quero encontrar profissionais e agendar serviços.',
    icon: require('@/assets/icons/profile-user.svg'),
    selectedIcon: require('@/assets/icons/profile-user-selected.svg'),
    action: 'Continuar como usuário',
    href: '/cadastro/usuario',
  },
  prestador: {
    title: 'Sou prestador',
    description: 'Quero oferecer serviços como profissional ou empresa.',
    icon: require('@/assets/icons/profile-company.svg'),
    selectedIcon: require('@/assets/icons/profile-company-selected.svg'),
    action: 'Continuar como prestador',
    href: '/cadastro/prestador',
  },
} as const;

type Profile = keyof typeof PROFILES;

export default function SignUpProfileScreen() {
  const { colors, fonts } = useAppTheme();
  const [profile, setProfile] = useState<Profile>('usuario');

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.surfaceContainerLowest }]}>
      <ScreenHeader />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.main}>
          <SignUpProgress label="Comece por aqui" step={1} totalSteps={2} />

          <Text
            variant="headlineSmall"
            accessibilityRole="header"
            style={[styles.title, { fontFamily: fonts.titleMedium.fontFamily }]}>
            Escolha seu perfil
          </Text>

          <View style={styles.options} accessibilityRole="radiogroup">
            {(Object.keys(PROFILES) as Profile[]).map((key) => (
              <ProfileOption
                key={key}
                {...PROFILES[key]}
                selected={profile === key}
                onPress={() => setProfile(key)}
              />
            ))}
          </View>
        </View>

        <View style={styles.actions}>
          <Button
            mode="contained"
            icon={ArrowRightIcon}
            contentStyle={styles.buttonContent}
            onPress={() => router.push(PROFILES[profile].href)}>
            {PROFILES[profile].action}
          </Button>

          <View style={styles.signInRow}>
            <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
              Já tem uma conta?
            </Text>
            <Button mode="text" onPress={() => router.dismissTo('/login')}>
              Entrar
            </Button>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

type ProfileOptionProps = {
  title: string;
  description: string;
  icon: ImageProps['source'];
  selectedIcon: ImageProps['source'];
  selected: boolean;
  onPress: () => void;
};

function ProfileOption({ title, description, icon, selectedIcon, selected, onPress }: ProfileOptionProps) {
  const { colors } = useAppTheme();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="radio"
      accessibilityState={{ checked: selected }}
      aria-checked={selected}
      style={[
        styles.option,
        selected
          ? { backgroundColor: colors.primaryContainer, borderColor: colors.primary, borderWidth: 2, padding: 19 }
          : { backgroundColor: colors.surfaceContainerLowest, borderColor: colors.outlineVariant },
      ]}>
      <View
        style={[
          styles.optionIllustration,
          { backgroundColor: selected ? colors.primary : colors.surfaceVariant },
        ]}>
        <Image source={selected ? selectedIcon : icon} style={styles.optionIcon} />
      </View>
      <View style={styles.optionContent}>
        <Text variant="titleMedium">{title}</Text>
        <Text variant="bodyMedium" style={{ color: colors.onSurfaceVariant }}>
          {description}
        </Text>
      </View>
      {selected ? (
        <Image source={require('@/assets/icons/radio-selected.svg')} style={styles.radio} />
      ) : (
        <View style={[styles.radio, styles.radioUnselected, { borderColor: colors.outline }]} />
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    flexGrow: 1,
    justifyContent: 'space-between',
    gap: 24,
    paddingTop: 20,
    paddingBottom: 24,
    paddingHorizontal: 20,
  },
  main: {
    gap: 24,
  },
  title: {
    textAlign: 'center',
  },
  options: {
    gap: 16,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    padding: 20,
    borderWidth: 1,
    borderRadius: 20,
    boxShadow: '0px 8px 24px rgba(0, 90, 193, 0.08)',
  },
  optionIllustration: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 50,
    height: 50,
    borderRadius: 14,
  },
  optionIcon: {
    width: 24,
    height: 24,
  },
  optionContent: {
    flex: 1,
    gap: 4,
  },
  radio: {
    width: 22,
    height: 22,
  },
  radioUnselected: {
    borderWidth: 2,
    borderRadius: 999,
  },
  actions: {
    gap: 12,
  },
  buttonContent: {
    flexDirection: 'row-reverse',
  },
  signInRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
  },
});
