import React from 'react';
import { View, StyleSheet, StatusBar, ScrollView } from 'react-native';
import { Header } from '@/components/ui/header';
import { TransactionList } from '@/components/ui/TransactionList';

export default function TransactionsScreen() {
    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />
            <Header title="लेन-देन इतिहास" showLanguageToggle={true} showBackButton={true} />

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.content}
                showsVerticalScrollIndicator={false}
            >
                <TransactionList />
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
    },
    scrollView: {
        flex: 1,
    },
    content: {
        paddingTop: 16,
        paddingBottom: 20,
    },
});
