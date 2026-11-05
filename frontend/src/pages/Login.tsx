import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Formik, Form } from 'formik';
import * as yup from 'yup';
import { Box, Button, CircularProgress, Container, Paper, TextField, Typography } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { clearError, login } from '../store/slices/authSlice';

// Reproduces reference frontend/src/pages/Login.tsx. D2: minimum password length 6 (reference: 8),
// aligned with registration so every registered user can sign in.
export const loginSchema = yup.object({
    email: yup.string().email('Enter a valid email').required('Email is required'),
    password: yup
        .string()
        .min(6, 'Password should be of minimum 6 characters length')
        .required('Password is required'),
});

const Login: React.FC = () => {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { loading, error } = useAppSelector((state) => state.auth);

    useEffect(() => {
        dispatch(clearError());
    }, [dispatch]);

    return (
        <Container component="main" maxWidth="xs">
            <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
                    <Typography component="h1" variant="h5" align="center" gutterBottom>
                        Sign in
                    </Typography>
                    <Formik
                        initialValues={{ email: '', password: '' }}
                        validationSchema={loginSchema}
                        onSubmit={async (values) => {
                            try {
                                await dispatch(login(values)).unwrap();
                                navigate('/');
                            } catch {
                                // Error is shown from the auth slice
                            }
                        }}
                    >
                        {({ values, errors, touched, handleChange, handleBlur }) => (
                            <Form noValidate>
                                <TextField
                                    fullWidth
                                    id="email"
                                    name="email"
                                    label="Email Address"
                                    value={values.email}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    error={touched.email && Boolean(errors.email)}
                                    helperText={touched.email && errors.email}
                                    margin="normal"
                                    disabled={loading}
                                />
                                <TextField
                                    fullWidth
                                    id="password"
                                    name="password"
                                    label="Password"
                                    type="password"
                                    value={values.password}
                                    onChange={handleChange}
                                    onBlur={handleBlur}
                                    error={touched.password && Boolean(errors.password)}
                                    helperText={touched.password && errors.password}
                                    margin="normal"
                                    disabled={loading}
                                />
                                {error && (
                                    <Typography color="error" align="center" sx={{ mt: 1 }}>
                                        {error}
                                    </Typography>
                                )}
                                <Button
                                    type="submit"
                                    fullWidth
                                    variant="contained"
                                    sx={{ mt: 3, mb: 2 }}
                                    disabled={loading}
                                >
                                    {loading ? <CircularProgress size={24} /> : 'Sign In'}
                                </Button>
                                <Button
                                    fullWidth
                                    variant="text"
                                    onClick={() => navigate('/register')}
                                    disabled={loading}
                                >
                                    Don't have an account? Sign Up
                                </Button>
                            </Form>
                        )}
                    </Formik>
                </Paper>
            </Box>
        </Container>
    );
};

export default Login;
