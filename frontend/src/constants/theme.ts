import { configureFonts, MD3LightTheme, useTheme } from 'react-native-paper';

const regular = { fontFamily: 'Roboto_400Regular', fontWeight: '400' } as const;
const medium = { fontFamily: 'Roboto_500Medium', fontWeight: '500' } as const;

// Escala tipográfica do frame "Design tokens (M3)" do Figma.
// No Figma só o label large tem espaçamento entre letras (0.1).
const typescale = configureFonts({
  config: {
    displayLarge: regular,
    displayMedium: regular,
    displaySmall: regular,
    headlineLarge: regular,
    headlineMedium: regular,
    headlineSmall: { ...regular, fontSize: 24, lineHeight: 32, letterSpacing: 0 },
    titleLarge: { ...regular, fontSize: 22, lineHeight: 28, letterSpacing: 0 },
    titleMedium: { ...medium, fontSize: 16, lineHeight: 24, letterSpacing: 0 },
    titleSmall: medium,
    bodyLarge: { ...regular, fontSize: 16, lineHeight: 24, letterSpacing: 0 },
    bodyMedium: { ...regular, fontSize: 14, lineHeight: 20, letterSpacing: 0 },
    bodySmall: { ...regular, fontSize: 12, lineHeight: 16, letterSpacing: 0 },
    labelLarge: { ...medium, fontSize: 14, lineHeight: 20, letterSpacing: 0.1 },
    labelMedium: medium,
    labelSmall: { ...medium, fontSize: 11, lineHeight: 16, letterSpacing: 0 },
  },
});

const fonts = { ...typescale, default: { ...typescale.default, ...regular } };

// Cores do frame "Design tokens (M3)" do Figma (esquema claro, seed #005AC1).
// Os papéis que não aparecem no Figma vêm do esquema M3 gerado pela mesma seed.
const colors = {
  ...MD3LightTheme.colors,
  primary: '#005AC1',
  onPrimary: '#FFFFFF',
  primaryContainer: '#D8E2FF',
  onPrimaryContainer: '#001A41',
  secondary: '#575E71',
  onSecondary: '#FFFFFF',
  secondaryContainer: '#DBE2F9',
  onSecondaryContainer: '#141B2C',
  tertiary: '#715573',
  onTertiary: '#FFFFFF',
  tertiaryContainer: '#FBD7FC',
  onTertiaryContainer: '#29132D',
  error: '#BA1A1A',
  onError: '#FFFFFF',
  errorContainer: '#FFDAD6',
  onErrorContainer: '#410002',
  background: '#FEFBFF',
  onBackground: '#1B1B1F',
  surface: '#FEFBFF',
  onSurface: '#1B1B1F',
  // "surfaceHighest" no Figma
  surfaceVariant: '#E1E2EC',
  onSurfaceVariant: '#44474F',
  outline: '#74777F',
  outlineVariant: '#C4C6D0',
  inverseSurface: '#303034',
  inverseOnSurface: '#F2F0F4',
  inversePrimary: '#ADC6FF',
  surfaceDisabled: 'rgba(27, 27, 31, 0.12)',
  onSurfaceDisabled: 'rgba(27, 27, 31, 0.38)',
  // Superfície com sobreposição de primary a 5%, 8%, 11%, 12% e 14%, como no M3
  elevation: {
    level0: 'transparent',
    level1: 'rgb(241, 243, 252)',
    level2: 'rgb(234, 238, 250)',
    level3: 'rgb(226, 233, 248)',
    level4: 'rgb(224, 232, 248)',
    level5: 'rgb(218, 228, 246)',
  },
  // Fora dos papéis que o Paper conhece
  success: '#146C2E',
  surfaceContainerLowest: '#FFFFFF',
};

export const theme = { ...MD3LightTheme, colors, fonts };

export type AppTheme = typeof theme;

export const useAppTheme = () => useTheme<AppTheme>();
