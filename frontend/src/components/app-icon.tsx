import { Image } from 'expo-image';

import { useAppTheme } from '@/constants/theme';

const icons = {
  calendar: require('@/assets/icons/calendar.svg'),
  check: require('@/assets/icons/check.svg'),
  home: require('@/assets/icons/login-home.svg'),
  pin: require('@/assets/icons/field-map-pin.svg'),
  user: require('@/assets/icons/profile-user.svg'),
  clipboard: require('@/assets/icons/clipboard.svg'),
  inbox: require('@/assets/icons/inbox.svg'),
  offline: require('@/assets/icons/cloud-off.svg'),
  refresh: require('@/assets/icons/refresh.svg'),
  info: require('@/assets/icons/info.svg'),
  lock: require('@/assets/icons/field-lock.svg'),
};

type AppIconProps = {
  name: keyof typeof icons;
  size?: number;
  color?: string;
};

export function AppIcon({ name, size = 24, color }: AppIconProps) {
  const { colors } = useAppTheme();
  return (
    <Image
      source={icons[name]}
      tintColor={color ?? colors.onSurfaceVariant}
      style={{ width: size, height: size }}
    />
  );
}
