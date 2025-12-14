/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import { Platform } from 'react-native';

const tintColorLight = '#0a7ea4';
const tintColorDark = '#fff';

export const Colors = {
  light: {
    text: '#212121',
    textSecondary: '#757575',
    textTertiary: '#9E9E9E',
    background: '#F5F5F5',
    card: '#FFFFFF',
    border: '#E0E0E0',
    tint: tintColorLight,
    icon: '#757575',
    tabIconDefault: '#757575',
    tabIconSelected: tintColorLight,
    primary: '#2E7D32',
    primaryLight: '#E8F5E9',
    secondary: '#F57C00',
    secondaryLight: '#FFF3E0',
    error: '#D32F2F',
    errorLight: '#FFEBEE',
    success: '#2E7D32', // Same as primary for now, but semantically distinct
    warning: '#F57C00',
    info: '#1976D2',
    infoLight: '#E3F2FD',
    white: '#FFFFFF',
    black: '#000000',
    shadow: '#000000',
    overlay: 'rgba(0, 0, 0, 0.5)',
    modalOverlay: 'rgba(0, 0, 0, 0.3)',
    primaryDisabled: '#A5D6A7',
    primaryContainer: '#F1F8E9',
    primaryVariant: '#4CAF50',
    transparent: 'transparent',
  },
  dark: {
    text: '#ECEDEE',
    textSecondary: '#9BA1A6',
    textTertiary: '#687076',
    background: '#151718',
    card: '#000000',
    border: '#333333',
    tint: tintColorDark,
    icon: '#9BA1A6',
    tabIconDefault: '#9BA1A6',
    tabIconSelected: tintColorDark,
    primary: '#4CAF50', // Lighter green for dark mode
    secondary: '#FFB74D', // Lighter orange
    error: '#EF5350', // Lighter red
    success: '#4CAF50',
    warning: '#FFB74D',
    info: '#4FC3F7',
    white: '#FFFFFF',
    black: '#000000',
    shadow: '#000000',
    overlay: 'rgba(0, 0, 0, 0.7)',
    modalOverlay: 'rgba(0, 0, 0, 0.5)',
  },
};

export const Fonts = Platform.select({
  ios: {
    /** iOS `UIFontDescriptorSystemDesignDefault` */
    sans: 'system-ui',
    /** iOS `UIFontDescriptorSystemDesignSerif` */
    serif: 'ui-serif',
    /** iOS `UIFontDescriptorSystemDesignRounded` */
    rounded: 'ui-rounded',
    /** iOS `UIFontDescriptorSystemDesignMonospaced` */
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
    serif: "Georgia, 'Times New Roman', serif",
    rounded: "'SF Pro Rounded', 'Hiragino Maru Gothic ProN', Meiryo, 'MS PGothic', sans-serif",
    mono: "SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace",
  },
});
