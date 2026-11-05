import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import client, { errorMessage } from '../../api/client';
import { Course } from '../../types';

// Mirrors reference frontend/src/store/slices/courseSlice.ts (same state shape and thunks used by the UI).

export interface CourseState {
  courses: Course[];
  currentCourse: Course | null;
  loading: boolean;
  error: string | null;
}

const initialState: CourseState = {
  courses: [],
  currentCourse: null,
  loading: false,
  error: null,
};

export const fetchCourses = createAsyncThunk<Course[], void, { rejectValue: string }>(
  'courses/fetchCourses',
  async (_, { rejectWithValue }) => {
    try {
      const response = await client.get<Course[]>('/courses');
      return response.data;
    } catch (error) {
      return rejectWithValue(errorMessage(error, 'Failed to fetch courses'));
    }
  }
);

export const enrollInCourse = createAsyncThunk<Course, string, { rejectValue: string }>(
  'courses/enrollInCourse',
  async (courseId, { rejectWithValue }) => {
    try {
      const response = await client.post<Course>(`/courses/${courseId}/enroll`);
      return response.data;
    } catch (error) {
      return rejectWithValue(errorMessage(error, 'Failed to enroll in course'));
    }
  }
);

const courseSlice = createSlice({
  name: 'courses',
  initialState,
  reducers: {
    setCurrentCourse: (state, action: { payload: Course | null }) => {
      state.currentCourse = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCourses.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCourses.fulfilled, (state, action) => {
        state.loading = false;
        state.courses = action.payload;
      })
      .addCase(fetchCourses.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? 'Failed to fetch courses';
      })
      .addCase(enrollInCourse.fulfilled, (state, action) => {
        const index = state.courses.findIndex((c) => c.id === action.payload.id);
        if (index !== -1) {
          state.courses[index] = action.payload;
        }
        if (state.currentCourse?.id === action.payload.id) {
          state.currentCourse = action.payload;
        }
      });
  },
});

export const { setCurrentCourse } = courseSlice.actions;
export default courseSlice.reducer;
