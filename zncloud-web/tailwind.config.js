/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9ff',
          100: '#dff2fe',
          200: '#b8e5fd',
          300: '#7cd1fb',
          400: '#5abff7',
          500: '#2ba0e8',
          600: '#1c82cd',
          700: '#1869a6',
          800: '#1a5989',
          900: '#1b4a71',
        },
        'brand-light': '#E8F4FD',
      },
    },
  },
  plugins: [],
}
