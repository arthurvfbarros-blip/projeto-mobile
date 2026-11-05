import { Image } from 'expo-image';
import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type CheckboxProps = {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  error?: string;
  disabled?: boolean;
};

export function Checkbox({ label, checked, onChange, error, disabled }: CheckboxProps) {
  const { colors } = useAppTheme();

  return (
    <View style={styles.container}>
      <Pressable
        onPress={() => onChange(!checked)}
        disabled={disabled}
        accessibilityRole="checkbox"
        accessibilityState={{ checked, disabled }}
        accessibilityLabel={label}
        style={styles.row}>
        <View
          style={[
            styles.box,
            {
              backgroundColor: colors.surfaceContainerLowest,
              borderColor: error ? colors.error : colors.outline,
            },
          ]}>
          {checked && <Image source={require('@/assets/icons/checkbox-check.svg')} style={styles.check} />}
        </View>
        <Text variant="bodyMedium" style={[styles.label, { color: colors.onSurfaceVariant }]}>
          {label}
        </Text>
      </Pressable>
      {error && (
        <Text variant="bodyMedium" style={{ color: colors.error }}>
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 8,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  box: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 22,
    height: 22,
    borderWidth: 1,
    borderRadius: 6,
  },
  check: {
    width: 14,
    height: 14,
  },
  label: {
    flex: 1,
  },
});
