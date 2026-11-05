import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { useAppTheme } from '@/constants/theme';

type SignUpProgressProps = {
  label: string;
  step: number;
  totalSteps: number;
};

export function SignUpProgress({ label, step, totalSteps }: SignUpProgressProps) {
  const { colors } = useAppTheme();

  return (
    <View
      style={styles.container}
      accessibilityRole="progressbar"
      accessibilityLabel={label}
      accessibilityValue={{ min: 0, max: totalSteps, now: step, text: `Etapa ${step} de ${totalSteps}` }}>
      <View style={styles.description}>
        <Text variant="labelLarge" style={{ color: colors.primary }}>
          {label}
        </Text>
        <Text variant="labelLarge" style={{ color: colors.onSurfaceVariant }}>
          {`${step} de ${totalSteps}`}
        </Text>
      </View>
      <View style={[styles.track, { backgroundColor: colors.surfaceVariant }]}>
        <View
          style={[
            styles.indicator,
            { backgroundColor: colors.primary, width: `${(step / totalSteps) * 100}%` },
          ]}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 8,
  },
  description: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  track: {
    height: 6,
    borderRadius: 999,
    overflow: 'hidden',
  },
  indicator: {
    height: 6,
    borderRadius: 999,
  },
});
