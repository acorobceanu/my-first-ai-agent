import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#176f6b',
      dark: '#0c4f4c',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#b4562b',
      dark: '#8e3f1d',
    },
    background: {
      default: '#f7f8f4',
      paper: '#ffffff',
    },
    text: {
      primary: '#1e2422',
      secondary: '#5c6864',
    },
    divider: '#dce2dd',
  },
  typography: {
    fontFamily: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: {
      fontSize: 'clamp(2rem, 4vw, 3.25rem)',
      fontWeight: 800,
      lineHeight: 1.05,
      letterSpacing: 0,
    },
    h2: {
      fontSize: 'clamp(1.5rem, 3vw, 2.25rem)',
      fontWeight: 750,
      lineHeight: 1.15,
      letterSpacing: 0,
    },
    h3: {
      fontSize: '1.25rem',
      fontWeight: 750,
      letterSpacing: 0,
    },
    button: {
      fontWeight: 700,
      letterSpacing: 0,
      textTransform: 'none',
    },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          minHeight: 44,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 16px 50px rgba(23, 44, 39, 0.12)',
        },
      },
    },
  },
});
