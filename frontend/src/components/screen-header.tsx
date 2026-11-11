import { Image } from 'expo-image';
import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type ScreenHeaderProps = {
  title?: string;
  onBack?: () => void;
  variant?: 'onboarding' | 'app';
  subtitle?: string;
};

export function ScreenHeader({
  title,
  onBack,
  variant = 'onboarding',
  subtitle,
}: ScreenHeaderProps) {
  const { colors } = useAppTheme();

  return (
    <View style={[styles.container, variant === 'app' && styles.appContainer]}>
      {onBack ? (
        <Pressable
          onPress={onBack}
          accessibilityRole="button"
          accessibilityLabel="Voltar"
          style={[
            styles.backButton,
            { borderColor: colors.surfaceVariant },
            variant === 'app' && styles.appBackButton,
          ]}>
          <Image source={require('@/assets/icons/arrow-left.svg')} style={styles.backIcon} />
        </Pressable>
      ) : variant === 'onboarding' ? (
        <View style={styles.spacer} />
      ) : null}
      {title && (
        <View style={variant === 'app' ? styles.appTitle : undefined}>
          <Text
            variant={variant === 'app' ? 'titleLarge' : 'titleMedium'}
            accessibilityRole="header">
            {title}
          </Text>
          {subtitle && (
            <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant }}>
              {subtitle}
            </Text>
          )}
        </View>
      )}
      {variant === 'onboarding' && <View style={styles.spacer} />}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 56,
    paddingHorizontal: 20,
  },
  backButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 40,
    height: 40,
    borderWidth: 1,
    borderRadius: 999,
  },
  backIcon: {
    width: 20,
    height: 20,
  },
  spacer: {
    width: 40,
  },
  appContainer: {
    height: 'auto',
    minHeight: 56,
    paddingHorizontal: 16,
    paddingVertical: 8,
    gap: 8,
    justifyContent: 'flex-start',
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
  },
  appBackButton: { borderWidth: 0, marginLeft: -8 },
  appTitle: { flex: 1, gap: 2 },
});
