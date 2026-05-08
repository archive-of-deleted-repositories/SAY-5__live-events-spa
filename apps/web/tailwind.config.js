/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#0b0d10',
        surface: '#13171c',
        muted: '#7a8693',
        accent: '#5fb1ff',
      },
    },
  },
  plugins: [],
};
