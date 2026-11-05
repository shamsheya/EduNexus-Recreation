import React from 'react';
import { Navigate } from 'react-router-dom';
import { CircularProgress } from '@mui/material';
import { useAppSelector } from '../store/hooks';

interface PrivateRouteProps {
  children: React.ReactNode;
}

// Same behaviour as the reference guard, plus waiting for session restore (token present, user loading).
const PrivateRoute: React.FC<PrivateRouteProps> = ({ children }) => {
  const { token, user, loading } = useAppSelector((state) => state.auth);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (loading || !user) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </div>
    );
  }

  return <>{children}</>;
};

export default PrivateRoute;
