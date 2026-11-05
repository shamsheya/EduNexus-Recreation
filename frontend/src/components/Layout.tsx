import React, { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import {
    AppBar,
    Avatar,
    Box,
    CssBaseline,
    Drawer,
    IconButton,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Toolbar,
    Typography,
} from '@mui/material';
import { Menu as MenuIcon, Dashboard, School, Person, Logout } from '@mui/icons-material';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/slices/authSlice';

// Reproduces reference frontend/src/components/Layout.tsx (240px drawer, AppBar avatar menu, 3 nav items).
// `ListItem button` (deprecated in MUI 5) is replaced by the equivalent ListItemButton.

const drawerWidth = 240;

const Layout: React.FC = () => {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { user } = useAppSelector((state) => state.auth);

    const [mobileOpen, setMobileOpen] = useState(false);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    const handleDrawerToggle = () => setMobileOpen(!mobileOpen);
    const handleMenu = (event: React.MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget);
    const handleClose = () => setAnchorEl(null);

    const handleLogout = () => {
        dispatch(logout());
        navigate('/login');
    };

    const menuItems = [
        { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard' },
        { text: 'Courses', icon: <School />, path: '/courses' },
        { text: 'Profile', icon: <Person />, path: '/profile' },
    ];

    const drawer = (
        <div>
            <Toolbar />
            <List>
                {menuItems.map((item) => (
                    <ListItemButton
                        key={item.text}
                        onClick={() => {
                            setMobileOpen(false);
                            navigate(item.path);
                        }}
                    >
                        <ListItemIcon>{item.icon}</ListItemIcon>
                        <ListItemText primary={item.text} />
                    </ListItemButton>
                ))}
            </List>
        </div>
    );

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />
            <AppBar
                position="fixed"
                sx={{
                    width: { sm: `calc(100% - ${drawerWidth}px)` },
                    ml: { sm: `${drawerWidth}px` },
                }}
            >
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        edge="start"
                        onClick={handleDrawerToggle}
                        sx={{ mr: 2, display: { sm: 'none' } }}
                    >
                        <MenuIcon />
                    </IconButton>
                    <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
                        EduNexus
                    </Typography>
                    <IconButton
                        onClick={handleMenu}
                        size="small"
                        sx={{ ml: 2 }}
                        aria-label="account menu"
                        aria-controls={anchorEl ? 'account-menu' : undefined}
                        aria-haspopup="true"
                        aria-expanded={anchorEl ? 'true' : undefined}
                    >
                        <Avatar sx={{ width: 32, height: 32 }}>
                            {user?.firstName?.[0]?.toUpperCase()}
                        </Avatar>
                    </IconButton>
                    <Menu
                        anchorEl={anchorEl}
                        id="account-menu"
                        open={Boolean(anchorEl)}
                        onClose={handleClose}
                        onClick={handleClose}
                        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                    >
                        <MenuItem onClick={() => navigate('/profile')}>
                            <ListItemIcon>
                                <Person fontSize="small" />
                            </ListItemIcon>
                            Profile
                        </MenuItem>
                        <MenuItem onClick={handleLogout}>
                            <ListItemIcon>
                                <Logout fontSize="small" />
                            </ListItemIcon>
                            Logout
                        </MenuItem>
                    </Menu>
                </Toolbar>
            </AppBar>
            <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={handleDrawerToggle}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', sm: 'none' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
                    }}
                >
                    {drawer}
                </Drawer>
                <Drawer
                    variant="permanent"
                    sx={{
                        display: { xs: 'none', sm: 'block' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
                    }}
                    open
                >
                    {drawer}
                </Drawer>
            </Box>
            <Box
                component="main"
                sx={{ flexGrow: 1, p: 3, width: { sm: `calc(100% - ${drawerWidth}px)` } }}
            >
                <Toolbar />
                <Outlet />
            </Box>
        </Box>
    );
};

export default Layout;
