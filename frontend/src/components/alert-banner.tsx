import { Image } from 'expo-image';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type AlertBannerProps = {
  children: string;
};

export function AlertBanner({ children }: AlertBannerProps) {
  const { colors } = useAppTheme();

  return (
    <View style={[styles.container, { backgroundColor: colors.errorContainer }]}>
      <Image source={require('@/assets/icons/banner-error.svg')} style={styles.icon} />
      <Text variant="bodyMedium" style={[styles.text, { color: colors.onErrorContainer }]}>
        {children}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 12,
  },
  icon: {
    width: 20,
    height: 20,
  },
  text: {
    flex: 1,
  },
});
