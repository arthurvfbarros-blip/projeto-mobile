import { StyleSheet, View } from 'react-native';

import { useAppTheme } from '@/constants/theme';

export function RequestListSkeleton({ label }: { label: string }) {
  const { colors } = useAppTheme();
  return (
    <View style={styles.container} accessibilityRole="progressbar" accessibilityLabel={label}>
      {[0, 1, 2].map((item) => (
        <View
          key={item}
          style={[
            styles.card,
            { borderColor: colors.outlineVariant, backgroundColor: colors.surface },
          ]}>
          <View style={[styles.line, styles.title, { backgroundColor: colors.surfaceVariant }]} />
          <View style={[styles.line, { backgroundColor: colors.surfaceVariant }]} />
          <View style={styles.footer}>
            <View style={[styles.tag, { backgroundColor: colors.surfaceVariant }]} />
            <View style={[styles.button, { backgroundColor: colors.surfaceVariant }]} />
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: 12 },
  card: { padding: 16, gap: 16, borderWidth: 1, borderRadius: 12 },
  line: { height: 16, borderRadius: 4, width: '65%' },
  title: { height: 20, width: '85%' },
  footer: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  tag: { height: 32, width: 80, borderRadius: 8 },
  button: { height: 40, width: 116, borderRadius: 20 },
});
