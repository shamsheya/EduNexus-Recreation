import React, { useEffect, useState } from 'react';
import { Avatar, Box, Button, Divider, Grid, Paper, TextField, Typography } from '@mui/material';
import client from '../api/client';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { updateProfile } from '../store/slices/authSlice';
import { User } from '../types';

// Reproduces reference frontend/src/pages/Profile.tsx. Uses the shared API client; the form
// re-syncs when the user is (re)loaded, so it is no longer blank after a page reload.
const Profile: React.FC = () => {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ firstName: '', lastName: '', email: '' });

  const resetForm = (u: User | null) =>
    setFormData({ firstName: u?.firstName || '', lastName: u?.lastName || '', email: u?.email || '' });

  useEffect(() => {
    if (!isEditing) resetForm(user);
  }, [user, isEditing]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await client.put<User>('/users/profile', formData);
      dispatch(updateProfile(response.data));
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update profile', error);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <Avatar sx={{ width: 100, height: 100, fontSize: '2.5rem', mr: 3 }}>
            {user?.firstName?.[0]?.toUpperCase()}
          </Avatar>
          <Box>
            <Typography variant="h4">
              {user?.firstName} {user?.lastName}
            </Typography>
            <Typography color="text.secondary">{user?.roles?.join(', ')}</Typography>
          </Box>
        </Box>

        <Divider sx={{ my: 3 }} />

        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="First Name"
                name="firstName"
                value={formData.firstName}
                onChange={handleInputChange}
                disabled={!isEditing}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Last Name"
                name="lastName"
                value={formData.lastName}
                onChange={handleInputChange}
                disabled={!isEditing}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                disabled={!isEditing}
              />
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2 }}>
                {isEditing ? (
                  <>
                    <Button type="submit" variant="contained" color="primary">
                      Save Changes
                    </Button>
                    <Button
                      variant="outlined"
                      onClick={() => {
                        setIsEditing(false);
                        resetForm(user);
                      }}
                    >
                      Cancel
                    </Button>
                  </>
                ) : (
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={(e) => {
                      e.preventDefault();
                      setIsEditing(true);
                    }}
                  >
                    Edit Profile
                  </Button>
                )}
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};

export default Profile;
