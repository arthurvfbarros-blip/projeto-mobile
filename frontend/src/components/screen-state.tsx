import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

import { AppIcon } from '@/components/app-icon';
import { useAppTheme } from '@/constants/theme';

type ScreenStateProps = {
  kind: 'empty' | 'error' | 'restricted';
  title: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function ScreenState({ kind, title, message, actionLabel, onAction }: ScreenStateProps) {
  const { colors } = useAppTheme();
  return (
    <View style={styles.container}>
      <View style={[styles.illustration, { backgroundColor: colors.surfaceVariant }]}>
        <AppIcon
          name={kind === 'empty' ? 'inbox' : kind === 'restricted' ? 'user' : 'offline'}
          size={40}
        />
      </View>
      <Text variant="titleMedium" accessibilityRole="header" style={styles.centered}>
        {title}
      </Text>
      <Text variant="bodyMedium" style={[styles.centered, { color: colors.onSurfaceVariant }]}>
        {message}
      </Text>
      {actionLabel && onAction && (
        <Button
          mode="outlined"
          style={styles.action}
          icon={
            kind === 'restricted'
              ? undefined
              : ({ color }) => <AppIcon name="refresh" color={color} size={18} />
          }
          onPress={onAction}>
          {actionLabel}
        </Button>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    gap: 16,
    paddingHorizontal: 8,
    paddingTop: 48,
    paddingBottom: 24,
  },
  illustration: {
    width: 88,
    height: 88,
    borderRadius: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  centered: { textAlign: 'center' },
  action: { marginTop: 0, paddingHorizontal: 8 },
});
