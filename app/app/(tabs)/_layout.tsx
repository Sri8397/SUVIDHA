import React from 'react';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Tabs } from 'expo-router';
import { View, Platform, StyleSheet } from 'react-native';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#2E7D32',
        tabBarInactiveTintColor: '#9E9E9E',
        headerShown: false,
        tabBarStyle: {
          backgroundColor: '#FFFFFF',
          height: Platform.OS === 'ios' ? 85 : 70,
          paddingBottom: Platform.OS === 'ios' ? 20 : 0,
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
          height: Platform.OS === 'ios' ? 65 : 70,
        },
        tabBarLabelStyle: {
          fontFamily: Platform.OS === 'ios' ? 'System' : 'Roboto',
          fontSize: 11,
          fontWeight: '600',
          paddingBottom: Platform.OS === 'ios' ? 0 : 10,
        },
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: 'होम',
          tabBarIcon: ({ color }) => (
            <FontAwesome name="home" size={22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="inventory"
        options={{
          title: 'इन्वेंटरी',
          tabBarIcon: ({ color }) => (
            <MaterialIcons name="inventory" size={22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="customers"
        options={{
          title: 'ग्राहक',
          tabBarIcon: ({ color }) => (
            <FontAwesome name="users" size={20} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="reports"
        options={{
          title: 'रिपोर्ट',
          tabBarIcon: ({ color }) => (
            <MaterialIcons name="pie-chart" size={22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="more"
        options={{
          title: 'अधिक',
          tabBarIcon: ({ color }) => (
            <FontAwesome name="bars" size={20} color={color} />
          ),
        }}
      />
    </Tabs>
  );
}
