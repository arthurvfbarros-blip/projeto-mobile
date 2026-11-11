import { Stack } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { AppNavigation } from '@/components/app-navigation';

export default function ProviderLayout() {
  return (
    <View style={styles.screen}>
      <View style={styles.stack}>
        <Stack screenOptions={{ headerShown: false }} />
      </View>
      <AppNavigation profile="prestador" />
    </View>
  );
}

const styles = StyleSheet.create({ screen: { flex: 1 }, stack: { flex: 1, overflow: 'hidden' } });
