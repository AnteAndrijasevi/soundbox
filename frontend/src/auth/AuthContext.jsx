import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { TOKEN_KEY, USER_KEY } from '../api/client';
import * as api from '../api';

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);

  const finishAuth = useCallback(async (token) => {
    localStorage.setItem(TOKEN_KEY, token);
    const { data } = await api.getMe();
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    setUser(data);
    return data;
  }, []);

  const login = useCallback(
    async (credentials) => {
      const { data } = await api.login(credentials);
      return finishAuth(data.token);
    },
    [finishAuth]
  );

  const register = useCallback(
    async (details) => {
      const { data } = await api.register(details);
      return finishAuth(data.token);
    },
    [finishAuth]
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const refreshUser = useCallback(async () => {
    const { data } = await api.getMe();
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    setUser(data);
    return data;
  }, []);

  const value = useMemo(
    () => ({ user, login, register, logout, refreshUser }),
    [user, login, register, logout, refreshUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
