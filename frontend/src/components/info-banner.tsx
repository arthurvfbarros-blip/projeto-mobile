import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { AppIcon } from '@/components/app-icon';
import { useAppTheme } from '@/constants/theme';

type InfoBannerProps = {
  title?: string;
  children: string;
  icon?: 'info' | 'lock';
};

export function InfoBanner({ title, children, icon }: InfoBannerProps) {
  const { colors } = useAppTheme();
  return (
    <View style={[styles.container, { backgroundColor: colors.primaryContainer }]}>
      {icon && <AppIcon name={icon} size={18} color={colors.onPrimaryContainer} />}
      <View style={styles.content}>
        {title && (
          <Text variant="labelLarge" style={{ color: colors.onPrimaryContainer }}>
            {title}
          </Text>
        )}
        <Text variant="bodyMedium" style={{ color: colors.onPrimaryContainer }}>
          {children}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 12 },
  content: { flex: 1, gap: 8 },
});
