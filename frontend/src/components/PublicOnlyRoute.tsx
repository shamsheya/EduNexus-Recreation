import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';

// D4: signed-in users visiting /login or /register are sent to the dashboard.
const PublicOnlyRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { token, user } = useAppSelector((state) => state.auth);
  if (token && user) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

export default PublicOnlyRoute;
