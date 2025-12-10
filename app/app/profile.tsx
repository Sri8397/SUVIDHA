import React, { useState } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';

export default function ProfileScreen() {
    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color="#212121" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>मेरी प्रोफाइल</Text>
                    <TouchableOpacity style={styles.notificationButton}>
                        <FontAwesome name="bell" size={20} color="#212121" />
                    </TouchableOpacity>
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Profile Section */}
                <View style={styles.profileSection}>
                    <View style={styles.profileImageContainer}>
                        <Image
                            source={{ uri: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=400' }}
                            style={styles.profileImage}
                        />
                    </View>
                    <Text style={styles.userName}>रमेश कुमार</Text>
                    <Text style={styles.storeName}>अपना स्टोर</Text>
                </View>

                {/* Editable Fields */}
                <View style={styles.fieldsContainer}>
                    {/* Phone Number */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>फ़ोन नंबर:</Text>
                            <Text style={styles.fieldValue}>9876543210</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Email */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>ईमेल:</Text>
                            <Text style={styles.fieldValue}>ramesh@apnastore.com</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Address */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>पता:</Text>
                            <Text style={styles.fieldValue}>शॉप नं. 12, जैन मार्केट, मुंबई</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>
                </View>

                {/* Account Settings Section */}
                <View style={styles.settingsSection}>
                    <Text style={styles.sectionTitle}>खाता सेटिंग</Text>

                    <TouchableOpacity style={styles.settingCard}>
                        <Text style={styles.settingText}>पासवर्ड बदलें</Text>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.settingCard}>
                        <Text style={styles.settingText}>ऐप सेटिंग</Text>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
    },
    headerSafeArea: {
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
    },
    notificationButton: {
        padding: 8,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 24,
    },
    profileSection: {
        alignItems: 'center',
        paddingVertical: 32,
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    profileImageContainer: {
        width: 120,
        height: 120,
        borderRadius: 60,
        overflow: 'hidden',
        marginBottom: 16,
        backgroundColor: '#FFFFFF',
        borderWidth: 3,
        borderColor: '#FFFFFF',
    },
    profileImage: {
        width: '100%',
        height: '100%',
    },
    userName: {
        fontSize: 24,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 4,
    },
    storeName: {
        fontSize: 16,
        color: '#757575',
    },
    fieldsContainer: {
        paddingHorizontal: 16,
        marginTop: 8,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#FFFFFF',
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    fieldContent: {
        flex: 1,
    },
    fieldLabel: {
        fontSize: 14,
        color: '#757575',
        marginBottom: 4,
    },
    fieldValue: {
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
    editIcon: {
        padding: 8,
    },
    settingsSection: {
        paddingHorizontal: 16,
        marginTop: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
    },
    settingCard: {
        backgroundColor: '#FFFFFF',
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    settingText: {
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
});
