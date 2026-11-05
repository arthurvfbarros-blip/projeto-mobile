import { Image } from 'expo-image';
import { StyleSheet } from 'react-native';

export function ArrowRightIcon() {
  return <Image source={require('@/assets/icons/arrow-right.svg')} style={styles.icon} />;
}

const styles = StyleSheet.create({
  icon: {
    width: 18,
    height: 18,
  },
});
