import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('ordercraft_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('ordercraft_token'));
  const [loading, setLoading] = useState(false);

  const login = async (usernameOrEmail, password) => {
    setLoading(true);
    try {
      const response = await api.post('/auth/login', { usernameOrEmail, password });
      const data = response.data;
      const userObj = {
        id: data.id,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        roles: data.roles || [],
      };
      localStorage.setItem('ordercraft_token', data.accessToken);
      localStorage.setItem('ordercraft_user', JSON.stringify(userObj));
      setToken(data.accessToken);
      setUser(userObj);
      return { success: true };
    } catch (err) {
      const msg = err.response?.data?.message || 'Authentication failed. Please verify credentials.';
      return { success: false, error: msg };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    try {
      await api.post('/auth/register', userData);
      // Immediately log in upon successful registration
      return await login(userData.username, userData.password);
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed.';
      return { success: false, error: msg };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('ordercraft_token');
    localStorage.removeItem('ordercraft_user');
    setUser(null);
    setToken(null);
    window.location.href = '/login';
  };

  const hasRole = (role) => {
    if (!user || !user.roles) return false;
    const cleanRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return user.roles.includes(cleanRole);
  };

  const isAdmin = hasRole('ADMIN');
  const isManager = hasRole('MANAGER') || isAdmin;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        loading,
        login,
        register,
        logout,
        hasRole,
        isAdmin,
        isManager,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
