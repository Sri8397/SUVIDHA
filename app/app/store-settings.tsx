import React, { useState } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';

export default function StoreSettingsScreen() {
    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color="#212121" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>स्टोर की सेटिंग</Text>
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
                {/* Editable Fields */}
                <View style={styles.fieldsContainer}>
                    {/* Store Name */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>दुकान का नाम</Text>
                            <Text style={styles.fieldValue}>अपना स्टोर</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Store Address */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>दुकान का पता</Text>
                            <Text style={styles.fieldValue}>शॉप नं. 12, जैन मार्केट, मुंबई</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Business Type */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>व्यवसाय का प्रकार</Text>
                            <Text style={styles.fieldValue}>खुदरा व्यापार</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* GST Details */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>जीएसटी विवरण</Text>
                            <Text style={styles.fieldValue}>27AABCU9603R1ZM</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Canvasing Settings */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>कानवासिंग सेट</Text>
                            <Text style={styles.fieldValue}>डिफ़ॉल्ट सेटिंग</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Store Rate */}
                    <TouchableOpacity style={styles.navigationCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>स्टोर की दुला</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={18} color="#757575" />
                    </TouchableOpacity>

                    {/* Language */}
                    <TouchableOpacity style={styles.navigationCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>भाषा</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={18} color="#757575" />
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
    fieldsContainer: {
        paddingHorizontal: 16,
        marginTop: 16,
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
    navigationCard: {
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
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
    fieldValue: {
        fontSize: 14,
        color: '#757575',
        marginTop: 4,
    },
    editIcon: {
        padding: 8,
    },
});
