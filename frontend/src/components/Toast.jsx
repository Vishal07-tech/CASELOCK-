import { Toaster, toast } from 'react-hot-toast';

/** App-wide toast notification host, mounted once in App.jsx. */
export function ToastHost() {
  return (
    <Toaster
      position="top-right"
      toastOptions={{
        duration: 4000,
        style: {
          fontSize: '14px',
          borderRadius: '10px',
          boxShadow: '0 4px 12px rgba(16,42,67,0.12)',
        },
        success: { iconTheme: { primary: '#16a34a', secondary: 'white' } },
        error: { iconTheme: { primary: '#dc2626', secondary: 'white' } },
      }}
    />
  );
}

export const notify = {
  success: (message) => toast.success(message),
  error: (message) => toast.error(message),
  info: (message) => toast(message),
};
