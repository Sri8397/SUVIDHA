import React from 'react';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Tabs } from 'expo-router';
import { Platform } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useAppMode } from '@/contexts/ModeContext';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export default function TabLayout() {
  const { t } = useTranslation();
  const { appMode } = useAppMode();
  const isPersonalMode = appMode === 'personal';
  const insets = useSafeAreaInsets();
  
  // Use the device's bottom safe area inset to properly account for home indicator
  const bottomInset = insets.bottom;
  
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#2E7D32',
        tabBarInactiveTintColor: '#9E9E9E',
        headerShown: false,
        tabBarStyle: {
          backgroundColor: '#FFFFFF',
          height: 60 + bottomInset,
          paddingBottom: bottomInset,
          borderTopWidth: 1,
          borderTopColor: '#E0E0E0',
          ...Platform.select({
            android: {
              elevation: 8,
            },
            ios: {
              shadowColor: '#000',
              shadowOffset: { width: 0, height: -2 },
              shadowOpacity: 0.1,
              shadowRadius: 4,
            },
          }),
        },
        tabBarItemStyle: {
          paddingVertical: 8,
          height: 60,
        },
        tabBarLabelStyle: {
          fontFamily: Platform.OS === 'ios' ? 'System' : 'Roboto',
          fontSize: 11,
          fontWeight: '600',
          paddingBottom: 4,
        },
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: t('tabs.home'),
          tabBarIcon: ({ color }) => (
            <FontAwesome name="home" size={22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="inventory"
        options={{
          title: t('inventory.title'),
          tabBarIcon: ({ color }) => (
            <MaterialIcons name="inventory" size={22} color={color} />
          ),
          // Hide inventory tab in personal mode
          href: isPersonalMode ? null : undefined,
        }}
      />
      <Tabs.Screen
        name="customers"
        options={{
          title: isPersonalMode ? t('personal.people') : t('customers.title'),
          tabBarIcon: ({ color }) => (
            <FontAwesome name="users" size={20} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="reports"
        options={{
          title: t('reports.title'),
          tabBarIcon: ({ color }) => (
            <MaterialIcons name="pie-chart" size={22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="more"
        options={{
          title: t('more.title'),
          tabBarIcon: ({ color }) => (
            <FontAwesome name="bars" size={20} color={color} />
          ),
        }}
      />
    </Tabs>
  );
}
