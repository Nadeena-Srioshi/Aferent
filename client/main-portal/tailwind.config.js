module.exports = {
  purge: [],
  darkMode: false, // or 'media' or 'class'
    content: [
    "./index.html",
    "./src/**/*.{vue,js,ts}"
  ],
  theme: {
    extend: {
      colors: {
        primary:   '#006064',   // main teal
        action:    '#00759A',   // hover / links
        ai:        '#4A55A2',   // AI symptom detector

        surface:   '#F4F7F8',   // page background
        card:      '#FFFFFF',   // card background
        border:    '#D1D9E0',   // subtle borders

        ink:       '#1F2937',   // primary text
        muted:     '#6B7280',   // secondary text

        success:   '#22C55E',
        warning:   '#F59E0B',
        danger:    '#EF4444',
      }
    },
  },
  theme: {
    extend: {},
  },
  variants: {
    extend: {},
  },
  plugins: [],
}
