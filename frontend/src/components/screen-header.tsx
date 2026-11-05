import { Image } from 'expo-image';
import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type ScreenHeaderProps = {
  title?: string;
  onBack?: () => void;
};

export function ScreenHeader({ title, onBack }: ScreenHeaderProps) {
  const { colors } = useAppTheme();

  return (
    <View style={styles.container}>
      {onBack ? (
        <Pressable
          onPress={onBack}
          accessibilityRole="button"
          accessibilityLabel="Voltar"
          style={[styles.backButton, { borderColor: colors.surfaceVariant }]}>
          <Image source={require('@/assets/icons/arrow-left.svg')} style={styles.backIcon} />
        </Pressable>
      ) : (
        <View style={styles.spacer} />
      )}
      {title && (
        <Text variant="titleMedium" accessibilityRole="header">
          {title}
        </Text>
      )}
      <View style={styles.spacer} />
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
});
