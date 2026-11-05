import { configureStore } from '@reduxjs/toolkit';
import authReducer, { logout } from './slices/authSlice';
import courseReducer from './slices/courseSlice';
import { setUnauthorizedHandler } from '../api/client';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    courses: courseReducer,
  },
});

setUnauthorizedHandler(() => store.dispatch(logout()));

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
