import { useEffect, useMemo, useState } from "react";
import { ThemeContext, PALETTES } from "./ThemeContext";

function readStored(key, fallback) {
  try {
    return localStorage.getItem(key) || fallback;
  } catch {
    return fallback;
  }
}

export function ThemeProvider({ children }) {
  const [mode, setMode] = useState(() => readStored("shopsearch-mode", "dark"));
  const [palette, setPalette] = useState(() => readStored("shopsearch-palette", "aurora"));
  const [favorites, setFavorites] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("shopsearch-favorites") || "[]");
    } catch {
      return [];
    }
  });

  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute("data-theme", mode);
    root.setAttribute("data-palette", palette);
    try {
      localStorage.setItem("shopsearch-mode", mode);
      localStorage.setItem("shopsearch-palette", palette);
    } catch {
      /* ignore */
    }
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) {
      meta.setAttribute("content", mode === "dark" ? "#0B1020" : "#F4F1FF");
    }
  }, [mode, palette]);

  useEffect(() => {
    try {
      localStorage.setItem("shopsearch-favorites", JSON.stringify(favorites));
    } catch {
      /* ignore */
    }
  }, [favorites]);

  const value = useMemo(
    () => ({
      mode,
      palette,
      palettes: PALETTES,
      toggleMode: () => setMode((current) => (current === "dark" ? "light" : "dark")),
      setPalette,
      favorites,
      isFavorite: (id) => favorites.includes(String(id)),
      toggleFavorite: (id) => {
        const key = String(id);
        setFavorites((current) =>
          current.includes(key) ? current.filter((item) => item !== key) : [...current, key]
        );
      },
    }),
    [mode, palette, favorites]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
