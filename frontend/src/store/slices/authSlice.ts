import { createAsyncThunk, createSlice, PayloadAction } from '@reduxjs/toolkit';
import client, { errorMessage, TOKEN_KEY } from '../../api/client';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../../types';
import { enrollInCourse } from './courseSlice';

// Mirrors reference frontend/src/store/slices/authSlice.ts; getCurrentUser is now actually used (D3).

export interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

const readToken = (): string | null => {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
};

export const initialState: AuthState = {
  user: null,
  token: readToken(),
  loading: false,
  error: null,
};

export const login = createAsyncThunk<AuthResponse, LoginRequest, { rejectValue: string }>(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await client.post<AuthResponse>('/auth/login', credentials);
      localStorage.setItem(TOKEN_KEY, response.data.token);
      return response.data;
    } catch (error) {
      return rejectWithValue(errorMessage(error, 'Login failed'));
    }
  }
);

export const register = createAsyncThunk<AuthResponse, RegisterRequest, { rejectValue: string }>(
  'auth/register',
  async (userData, { rejectWithValue }) => {
    try {
      const response = await client.post<AuthResponse>('/auth/register', userData);
      localStorage.setItem(TOKEN_KEY, response.data.token);
      return response.data;
    } catch (error) {
      return rejectWithValue(errorMessage(error, 'Registration failed'));
    }
  }
);

export const getCurrentUser = createAsyncThunk<User, void, { rejectValue: string }>(
  'auth/getCurrentUser',
  async (_, { rejectWithValue }) => {
    try {
      const response = await client.get<User>('/auth/me');
      return response.data;
    } catch (error) {
      return rejectWithValue(errorMessage(error, 'Failed to fetch user'));
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    updateProfile: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.error = null;
      localStorage.removeItem(TOKEN_KEY);
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.token;
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? 'Login failed';
      })
      .addCase(register.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(register.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.token;
      })
      .addCase(register.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? 'Registration failed';
      })
      // Keep the Dashboard "Enrolled Courses" count current after enrolling.
      .addCase(enrollInCourse.fulfilled, (state, action) => {
        if (state.user && !state.user.enrolledCourses.includes(action.payload.id)) {
          state.user.enrolledCourses.push(action.payload.id);
        }
      })
      .addCase(getCurrentUser.pending, (state) => {
        state.loading = true;
      })
      .addCase(getCurrentUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
      })
      .addCase(getCurrentUser.rejected, (state) => {
        // Session could not be restored (expired token, deleted user, backend down): sign out
        // so the guard sends the user to /login instead of waiting forever.
        state.loading = false;
        state.token = null;
        state.user = null;
        localStorage.removeItem(TOKEN_KEY);
      });
  },
});

export const { updateProfile, logout, clearError } = authSlice.actions;
export default authSlice.reducer;
