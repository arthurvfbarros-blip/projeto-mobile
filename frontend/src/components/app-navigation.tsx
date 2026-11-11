import { router, usePathname } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppIcon } from '@/components/app-icon';
import { useAppTheme } from '@/constants/theme';

const destinations = {
  cliente: [
    { href: '/cliente', label: 'Início', icon: 'home' },
    { href: '/cliente/pedidos', label: 'Pedidos', icon: 'clipboard' },
    { href: '/cliente/enderecos', label: 'Endereços', icon: 'pin' },
  ],
  prestador: [
    { href: '/prestador/oportunidades', label: 'Oportunidades', icon: 'clipboard' },
    { href: '/prestador', label: 'Perfil', icon: 'user' },
  ],
} as const;

export function AppNavigation({ profile }: { profile: keyof typeof destinations }) {
  const { colors } = useAppTheme();
  const pathname = usePathname();
  if (pathname.endsWith('/novo')) return null;

  return (
    <SafeAreaView edges={['bottom']} style={{ backgroundColor: colors.surfaceContainerLowest }}>
      <View
        style={[styles.bar, { borderTopColor: colors.surfaceVariant }]}
        accessibilityRole="tablist">
        {destinations[profile].map((item) => {
          const selected =
            pathname === item.href ||
            (item.href !== '/cliente' &&
              item.href !== '/prestador' &&
              pathname.startsWith(item.href + '/'));
          return (
            <Pressable
              key={item.href}
              accessibilityRole="tab"
              accessibilityLabel={item.label}
              accessibilityState={{ selected }}
              aria-selected={selected}
              onPress={() => router.navigate(item.href)}
              style={styles.item}>
              <View
                style={[
                  styles.indicator,
                  selected && { backgroundColor: colors.primaryContainer },
                ]}>
                <AppIcon
                  name={item.icon}
                  color={selected ? colors.onPrimaryContainer : colors.onSurfaceVariant}
                />
              </View>
              <Text
                variant="labelMedium"
                style={{ color: selected ? colors.onSurface : colors.onSurfaceVariant }}>
                {item.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    width: '100%',
    maxWidth: 560,
    alignSelf: 'center',
    paddingHorizontal: 12,
    paddingTop: 8,
    paddingBottom: 12,
    borderTopWidth: 1,
  },
  item: { flex: 1, alignItems: 'center', gap: 4, paddingVertical: 4 },
  indicator: {
    height: 32,
    minWidth: 64,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
