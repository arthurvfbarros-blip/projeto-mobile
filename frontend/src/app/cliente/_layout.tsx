import { Stack } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { AppNavigation } from '@/components/app-navigation';
import { OrderDraftProvider } from '@/providers/order-draft-provider';

export default function ClientLayout() {
  return (
    <OrderDraftProvider>
      <View style={styles.screen}>
        <View style={styles.stack}>
          <Stack screenOptions={{ headerShown: false }} />
        </View>
        <AppNavigation profile="cliente" />
      </View>
    </OrderDraftProvider>
  );
}

const styles = StyleSheet.create({ screen: { flex: 1 }, stack: { flex: 1, overflow: 'hidden' } });
