import React, { useEffect } from 'react';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { Provider } from 'react-redux';
import { store } from './store';
import { useAppDispatch, useAppSelector } from './store/hooks';
import { getCurrentUser } from './store/slices/authSlice';
import theme from './theme';
import PrivateRoute from './components/PrivateRoute';
import PublicOnlyRoute from './components/PublicOnlyRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import CourseList from './pages/CourseList';
import CourseDetail from './pages/CourseDetail';
import Profile from './pages/Profile';

/** D3: restore the signed-in user after a reload (the reference never dispatched getCurrentUser). */
const SessionBootstrap: React.FC = () => {
  const dispatch = useAppDispatch();
  const { token, user, loading } = useAppSelector((state) => state.auth);
  useEffect(() => {
    if (token && !user && !loading) {
      dispatch(getCurrentUser());
    }
  }, [dispatch, token, user, loading]);
  return null;
};

// Route table identical to reference App.tsx:24-40.
export const AppRoutes: React.FC = () => (
  <Routes>
    <Route path="/login" element={<PublicOnlyRoute><Login /></PublicOnlyRoute>} />
    <Route path="/register" element={<PublicOnlyRoute><Register /></PublicOnlyRoute>} />

    <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
      <Route index element={<Navigate to="/dashboard" replace />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="courses" element={<CourseList />} />
      <Route path="courses/:id" element={<CourseDetail />} />
      <Route path="profile" element={<Profile />} />
    </Route>

    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
);

const App: React.FC = () => (
  <Provider store={store}>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SessionBootstrap />
      <Router>
        <AppRoutes />
      </Router>
    </ThemeProvider>
  </Provider>
);

export default App;
