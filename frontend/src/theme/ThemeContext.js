import { createContext, useContext } from "react";

export const PALETTES = [
  { id: "aurora", label: "Aurora", swatch: "#6C5CE7" },
  { id: "coral", label: "Coral", swatch: "#FF6B6B" },
  { id: "jade", label: "Jade", swatch: "#10B981" },
];

export const ThemeContext = createContext(null);

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error("useTheme must be used within ThemeProvider");
  }
  return ctx;
}
