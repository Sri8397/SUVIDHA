import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface User {
    phone: string;
    name?: string;
}

interface AuthContextType {
    isAuthenticated: boolean;
    isLoading: boolean;
    user: User | null;
    login: (phone: string) => Promise<void>;
    signup: (name: string, phone: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const AUTH_STORAGE_KEY = '@suvidha_auth';

export function AuthProvider({ children }: { children: ReactNode }) {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState<User | null>(null);

    // Load auth state on mount
    useEffect(() => {
        loadAuthState();
    }, []);

    const loadAuthState = async () => {
        try {
            const stored = await AsyncStorage.getItem(AUTH_STORAGE_KEY);
            if (stored) {
                const data = JSON.parse(stored);
                setUser(data.user);
                setIsAuthenticated(true);
            }
        } catch (error) {
            console.error('Failed to load auth state:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const login = async (phone: string) => {
        const userData: User = { phone };
        setUser(userData);
        setIsAuthenticated(true);
        await AsyncStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ user: userData }));
    };

    const signup = async (name: string, phone: string) => {
        const userData: User = { name, phone };
        setUser(userData);
        setIsAuthenticated(true);
        await AsyncStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ user: userData }));
    };

    const logout = async () => {
        setUser(null);
        setIsAuthenticated(false);
        await AsyncStorage.removeItem(AUTH_STORAGE_KEY);
    };

    return (
        <AuthContext.Provider value={{ isAuthenticated, isLoading, user, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
