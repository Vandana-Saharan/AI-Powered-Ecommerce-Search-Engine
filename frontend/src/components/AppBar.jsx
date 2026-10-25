import { useNavigate } from "react-router-dom";
import { useTheme } from "../theme/ThemeContext";

function IconSun() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
    </svg>
  );
}

function IconMoon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M21 14.3A8.5 8.5 0 1 1 9.7 3a7 7 0 0 0 11.3 11.3z" />
    </svg>
  );
}

function AppBar({ showBack = false }) {
  const navigate = useNavigate();
  const { mode, toggleMode, palette, setPalette, palettes } = useTheme();

  return (
    <header className={`app-bar glass-panel ${showBack ? "has-back" : ""}`}>
      <div className="app-bar-inner">
        <div className="app-bar-leading">
          {showBack && (
            <button
              className="md-btn md-btn-tonal back-btn"
              onClick={() => navigate(-1)}
              aria-label="Go back to search results"
              id="back-to-search-btn"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" aria-hidden="true">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              Back
            </button>
          )}
          <button className="brand-lockup" onClick={() => navigate("/")} aria-label="ShopSearch home">
            <span className="brand-mark" aria-hidden="true">S</span>
            <span className="brand-name">ShopSearch</span>
          </button>
        </div>

        <div className="app-bar-actions">
          <div className="palette-toggle" role="radiogroup" aria-label="Color palette">
            {palettes.map((item) => (
              <button
                key={item.id}
                type="button"
                role="radio"
                aria-checked={palette === item.id}
                aria-label={`${item.label} palette`}
                className={`palette-swatch ${palette === item.id ? "is-active" : ""}`}
                style={{ "--swatch": item.swatch }}
                onClick={() => setPalette(item.id)}
                title={item.label}
              />
            ))}
          </div>

          <button
            type="button"
            className="theme-toggle"
            onClick={toggleMode}
            aria-label={mode === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          >
            <span className={`theme-toggle-thumb ${mode}`}>
              {mode === "dark" ? <IconMoon /> : <IconSun />}
            </span>
            <span className="theme-toggle-label">{mode === "dark" ? "Dark" : "Light"}</span>
          </button>
        </div>
      </div>
    </header>
  );
}

export default AppBar;
