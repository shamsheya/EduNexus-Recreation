import React, { useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Card, CardContent, CircularProgress, Grid, Link, Typography } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { enrollInCourse, fetchCourses } from '../store/slices/courseSlice';

// Reproduces reference frontend/src/pages/CourseList.tsx.
// D6: the card title links to the course detail page (the reference had no path to it).
// Enrolment failures are still reported with alert(), as in the reference (CourseList.tsx:29,33).
const CourseList: React.FC = () => {
  const dispatch = useAppDispatch();
  const { courses, loading } = useAppSelector((state) => state.courses);

  useEffect(() => {
    dispatch(fetchCourses());
  }, [dispatch]);

  const handleEnroll = async (courseId: string) => {
    try {
      await dispatch(enrollInCourse(courseId)).unwrap();
      dispatch(fetchCourses());
    } catch (message) {
      alert(typeof message === 'string' ? message : 'Failed to enroll in course');
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Available Courses
      </Typography>
      <Grid container spacing={3}>
        {courses.map((course) => (
          <Grid item xs={12} sm={6} md={4} key={course.id}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  <Link component={RouterLink} to={`/courses/${course.id}`} underline="hover" color="inherit">
                    {course.title}
                  </Link>
                </Typography>
                <Typography color="text.secondary" paragraph>
                  {course.description}
                </Typography>
                <Button variant="contained" color="primary" onClick={() => handleEnroll(course.id)}>
                  Enroll
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default CourseList;
