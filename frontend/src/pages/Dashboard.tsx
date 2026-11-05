import React from 'react';
import { Box, Grid, Paper, Typography } from '@mui/material';
import { useAppSelector } from '../store/hooks';

// Reproduces reference frontend/src/pages/Dashboard.tsx (3 stat Papers + static Recent Activity).
const Dashboard: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Welcome, {user?.firstName}!
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Enrolled Courses
            </Typography>
            <Typography variant="h3">{user?.enrolledCourses?.length || 0}</Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Managed Courses
            </Typography>
            <Typography variant="h3">{user?.managedCourses?.length || 0}</Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Role
            </Typography>
            <Typography variant="h3">{user?.roles?.join(', ')}</Typography>
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>
            <Typography color="text.secondary">No recent activity to display.</Typography>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
