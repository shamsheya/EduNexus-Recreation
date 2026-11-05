import React, { useEffect } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useFormik } from 'formik';
import * as yup from 'yup';
import {
    Alert,
    Avatar,
    Box,
    Button,
    Container,
    FormControl,
    InputLabel,
    Link,
    MenuItem,
    Paper,
    Select,
    TextField,
    Typography,
} from '@mui/material';
import { PersonAddOutlined } from '@mui/icons-material';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { clearError, register } from '../store/slices/authSlice';
import { UserRole } from '../types';

// Reproduces reference frontend/src/pages/Register.tsx. The Admin option stays visible (fidelity);
// the backend rejects admin self-registration and its message is shown in the Alert (decision OD1).
export const registerSchema = yup.object({
    email: yup.string().email('Enter a valid email').required('Email is required'),
    password: yup
        .string()
        .min(6, 'Password should be of minimum 6 characters length')
        .required('Password is required'),
    firstName: yup.string().required('First name is required'),
    lastName: yup.string().required('Last name is required'),
    role: yup.string().oneOf(['student', 'instructor', 'admin'], 'Invalid role').required('Role is required'),
});

const Register: React.FC = () => {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { loading, error } = useAppSelector((state) => state.auth);

    useEffect(() => {
        dispatch(clearError());
    }, [dispatch]);

    const formik = useFormik({
        initialValues: {
            email: '',
            password: '',
            firstName: '',
            lastName: '',
            role: 'student' as UserRole,
        },
        validationSchema: registerSchema,
        onSubmit: async (values) => {
            try {
                await dispatch(register(values)).unwrap();
                navigate('/');
            } catch {
                // Error is shown from the auth slice
            }
        },
    });

    return (
        <Container component="main" maxWidth="xs">
            <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Paper
                    elevation={3}
                    sx={{ padding: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', width: '100%' }}
                >
                    <Avatar sx={{ m: 1, bgcolor: 'secondary.main' }}>
                        <PersonAddOutlined />
                    </Avatar>
                    <Typography component="h1" variant="h5">
                        Create an Account
                    </Typography>
                    {error && (
                        <Alert severity="error" sx={{ mt: 2, width: '100%' }}>
                            {error}
                        </Alert>
                    )}
                    <Box component="form" noValidate onSubmit={formik.handleSubmit} sx={{ mt: 1, width: '100%' }}>
                        <TextField
                            margin="normal"
                            fullWidth
                            id="firstName"
                            name="firstName"
                            label="First Name"
                            autoComplete="given-name"
                            autoFocus
                            value={formik.values.firstName}
                            onChange={formik.handleChange}
                            error={formik.touched.firstName && Boolean(formik.errors.firstName)}
                            helperText={formik.touched.firstName && formik.errors.firstName}
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            id="lastName"
                            name="lastName"
                            label="Last Name"
                            autoComplete="family-name"
                            value={formik.values.lastName}
                            onChange={formik.handleChange}
                            error={formik.touched.lastName && Boolean(formik.errors.lastName)}
                            helperText={formik.touched.lastName && formik.errors.lastName}
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            id="email"
                            name="email"
                            label="Email Address"
                            autoComplete="email"
                            value={formik.values.email}
                            onChange={formik.handleChange}
                            error={formik.touched.email && Boolean(formik.errors.email)}
                            helperText={formik.touched.email && formik.errors.email}
                        />
                        <TextField
                            margin="normal"
                            fullWidth
                            name="password"
                            label="Password"
                            type="password"
                            id="password"
                            autoComplete="new-password"
                            value={formik.values.password}
                            onChange={formik.handleChange}
                            error={formik.touched.password && Boolean(formik.errors.password)}
                            helperText={formik.touched.password && formik.errors.password}
                        />
                        <FormControl fullWidth margin="normal">
                            <InputLabel id="role-label">Role</InputLabel>
                            <Select
                                labelId="role-label"
                                id="role"
                                name="role"
                                value={formik.values.role}
                                label="Role"
                                onChange={formik.handleChange}
                            >
                                <MenuItem value="student">Student</MenuItem>
                                <MenuItem value="instructor">Instructor</MenuItem>
                                <MenuItem value="admin">Admin</MenuItem>
                            </Select>
                        </FormControl>
                        <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2 }} disabled={loading}>
                            {loading ? 'Creating Account...' : 'Create Account'}
                        </Button>
                        <Box sx={{ textAlign: 'center' }}>
                            <Link component={RouterLink} to="/login" variant="body2">
                                {'Already have an account? Sign In'}
                            </Link>
                        </Box>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default Register;
