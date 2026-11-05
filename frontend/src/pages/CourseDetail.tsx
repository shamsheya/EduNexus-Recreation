import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';
import {
  Box,
  Button,
  CircularProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import {
  Announcement as AnnouncementIcon,
  Assignment as AssignmentIcon,
  Description as DescriptionIcon,
  Forum as ForumIcon,
  Quiz as QuizIcon,
  VideoLibrary as VideoIcon,
} from '@mui/icons-material';
import client from '../api/client';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { enrollInCourse } from '../store/slices/courseSlice';
import { Course } from '../types';
import ErrorMessage from '../components/ErrorMessage';

// Reproduces reference frontend/src/pages/CourseDetail.tsx (title, description, Enroll button,
// Modules | Announcements | Discussions tabs). Uses the shared API client instead of raw fetch().

export const getModuleIcon = (type: string) => {
  switch (type) {
    case 'assignment':
      return <AssignmentIcon />;
    case 'quiz':
      return <QuizIcon />;
    case 'video':
      return <VideoIcon />;
    default:
      return <DescriptionIcon />;
  }
};

const CourseDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const [course, setCourse] = useState<Course | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const fetchCourse = async () => {
      setLoading(true);
      setLoadError(null);
      try {
        const response = await client.get<Course>(`/courses/${id}`);
        if (!cancelled) setCourse(response.data);
      } catch (error) {
        if (cancelled) return;
        setCourse(null);
        // 404 keeps the reference's "Course not found" text; other failures get an error panel.
        if (!(axios.isAxiosError(error) && error.response?.status === 404)) {
          setLoadError('Failed to load course');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchCourse();
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (loadError) {
    return (
      <Box sx={{ p: 3 }}>
        <ErrorMessage message={loadError} />
      </Box>
    );
  }

  if (!course) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography>Course not found</Typography>
      </Box>
    );
  }

  const userId = user?.id || '';
  const isEnrolled = course.enrolledStudents.includes(userId);
  const isInstructor = course.adminId === userId || course.teachingAssistants.includes(userId);

  const handleEnroll = async () => {
    try {
      const updated = await dispatch(enrollInCourse(course.id)).unwrap();
      setCourse(updated);
    } catch (message) {
      console.error('Error enrolling in course:', message);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        {course.title}
      </Typography>
      <Typography color="text.secondary" paragraph>
        {course.description}
      </Typography>

      {!isEnrolled && !isInstructor && (
        <Button variant="contained" color="primary" sx={{ mb: 3 }} onClick={handleEnroll}>
          Enroll in Course
        </Button>
      )}

      <Paper sx={{ mb: 3 }}>
        <Tabs value={tabValue} onChange={(_e, v: number) => setTabValue(v)}>
          <Tab label="Modules" />
          <Tab label="Announcements" />
          <Tab label="Discussions" />
        </Tabs>

        <Box sx={{ p: 2 }}>
          {tabValue === 0 && (
            <List>
              {course.modules.map((module) => (
                <ListItem key={module.id}>
                  <ListItemIcon>{getModuleIcon(module.contents[0]?.type || '')}</ListItemIcon>
                  <ListItemText
                    primary={
                      <Typography variant="h6" component="div">
                        {module.title}
                      </Typography>
                    }
                    secondary={
                      <Typography component="span" variant="body2" color="text.primary">
                        {module.description}
                      </Typography>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}

          {tabValue === 1 && (
            <List>
              {course.announcements.map((announcement) => (
                <ListItem key={announcement.id}>
                  <ListItemIcon>
                    <AnnouncementIcon />
                  </ListItemIcon>
                  <ListItemText
                    primary={announcement.title}
                    secondary={
                      <>
                        <Typography component="span" variant="body2" color="text.primary">
                          {announcement.content}
                        </Typography>
                        <Typography component="span" variant="body2" color="text.secondary">
                          {' - '}
                          {new Date(announcement.createdAt).toLocaleDateString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}

          {tabValue === 2 && (
            <List>
              {course.discussions.map((discussion) => (
                <ListItem key={discussion.id}>
                  <ListItemIcon>
                    <ForumIcon />
                  </ListItemIcon>
                  <ListItemText
                    primary={discussion.title}
                    secondary={
                      <>
                        <Typography component="span" variant="body2" color="text.primary">
                          {discussion.content}
                        </Typography>
                        <Typography component="span" variant="body2" color="text.secondary">
                          {' - '}
                          {new Date(discussion.createdAt).toLocaleDateString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Paper>
    </Box>
  );
};

export default CourseDetail;
