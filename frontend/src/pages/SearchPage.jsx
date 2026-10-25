import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppBar from "../components/AppBar";
import { useTheme } from "../theme/ThemeContext";
import "../App.css";

const SUGGESTIONS = [
  "party wear for men",
  "shoes for men",
  "Apple under 2000",
  "red running shoes",
  "perfume for evening",
  "luxury watches",
  "women dresses",
];

function IconSearch({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.35-4.35" />
    </svg>
  );
}

function IconArrow() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true">
      <path d="M5 12h14M12 5l7 7-7 7" />
    </svg>
  );
}

function IconStar() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

function IconHeart({ filled }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  );
}

function SkeletonCard() {
  return (
    <div className="skeleton-card glass-panel" aria-hidden="true">
      <div className="skeleton-line img" />
      <div className="skeleton-body">
        <div className="skeleton-line sm" />
        <div className="skeleton-line md" />
        <div className="skeleton-line lg" />
        <div style={{ display: "flex", justifyContent: "space-between", marginTop: "8px" }}>
          <div className="skeleton-line price" />
          <div className="skeleton-line rating" />
        </div>
      </div>
    </div>
  );
}

function SearchPage() {
  const [query, setQuery] = useState("");
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [hasSearched, setHasSearched] = useState(false);
  const [sortBy, setSortBy] = useState("relevance");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [useAi, setUseAi] = useState(true);

  const navigate = useNavigate();
  const { isFavorite, toggleFavorite } = useTheme();

  const searchProducts = async (term = query, overrideAi = useAi) => {
    const nextQuery = term.trim();
    if (!nextQuery) return;

    setQuery(nextQuery);
    setLoading(true);
    setError("");
    setHasSearched(true);
    setCategoryFilter("all");
    setSortBy("relevance");

    try {
      const endpoint = overrideAi
        ? `http://localhost:8080/api/products/search/ai?q=${encodeURIComponent(nextQuery)}`
        : `http://localhost:8080/api/products/search?q=${encodeURIComponent(nextQuery)}`;

      const response = await fetch(endpoint);

      if (!response.ok) {
        throw new Error("Failed to fetch products");
      }

      const data = await response.json();
      if (overrideAi && data && data.products) {
        setProducts(data.products);
      } else {
        setProducts(Array.isArray(data) ? data : (data.products || []));
      }
    } catch (err) {
      console.error("Search API Error:", err);
      setError("Something went wrong while searching.");
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const categories = useMemo(() => {
    const unique = [...new Set(products.map((item) => item.category).filter(Boolean))];
    return unique.sort();
  }, [products]);

  const visibleProducts = useMemo(() => {
    let next = [...products];
    if (categoryFilter !== "all") {
      next = next.filter((item) => item.category === categoryFilter);
    }
    if (sortBy === "price-asc") next.sort((a, b) => a.price - b.price);
    if (sortBy === "price-desc") next.sort((a, b) => b.price - a.price);
    if (sortBy === "rating") next.sort((a, b) => (b.rating || 0) - (a.rating || 0));
    return next;
  }, [products, categoryFilter, sortBy]);

  const showIdle = !hasSearched && !loading;
  const showEmpty = hasSearched && !loading && !error && products.length === 0;
  const showError = !loading && !!error;
  const showCards = !loading && !error && products.length > 0;

  return (
    <div className="search-page">
      <AppBar />

      <section className="search-hero">
        <div className="hero-badge glass-chip">
          <span className="badge-dot" aria-hidden="true" />
          Material search · glass surfaces
        </div>

        <h1 className="hero-title">
          Find your perfect
          <span className="accent-line">product instantly</span>
        </h1>

        <p className="hero-subtitle">
          Full-text discovery with a tactile Material layer: chips, elevation, and frosted glass.
        </p>

        <div className="search-bar-wrapper glass-panel" role="search">
          <label htmlFor="search-input" className="sr-only">Search products</label>
          <span className="search-icon">
            <IconSearch size={18} />
          </span>

          <input
            id="search-input"
            className="search-input"
            type="text"
            placeholder="Search laptops, shoes, headphones…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") searchProducts();
            }}
            autoComplete="off"
            spellCheck="false"
            aria-label="Search products"
          />

          {query && (
            <button
              type="button"
              className="clear-btn"
              onClick={() => setQuery("")}
              aria-label="Clear search"
            >
              ×
            </button>
          )}

          <button
            id="search-button"
            className="search-btn md-ripple"
            onClick={() => searchProducts()}
            disabled={loading || !query.trim()}
            aria-label="Search"
          >
            {loading ? (
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ animation: "spin 0.8s linear infinite" }} aria-hidden="true">
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
            ) : (
              <IconSearch size={16} />
            )}
            <span>{loading ? "Searching…" : "Search"}</span>
          </button>
        </div>

        <div className="ai-toggle-wrapper">
          <button
            type="button"
            className={`ai-toggle-btn ${useAi ? "is-active" : ""}`}
            onClick={() => setUseAi((prev) => !prev)}
            aria-pressed={useAi}
            title="Toggle between AI-Powered Natural Language Search and Traditional Keyword Search"
          >
            <span>✨ AI Natural Language Search</span>
            <span style={{ fontSize: "11px", opacity: 0.85 }}>{useAi ? "ON" : "OFF"}</span>
          </button>
        </div>

        <div className="suggestion-row" aria-label="Popular searches">
          {SUGGESTIONS.map((term) => (
            <button
              key={term}
              type="button"
              className={`md-chip ${query === term ? "is-selected" : ""}`}
              onClick={() => searchProducts(term)}
            >
              {term}
            </button>
          ))}
        </div>
      </section>

      {loading && (
        <section className="loading-section" aria-live="polite" aria-label="Loading results">
          <div className="skeleton-grid">
            {Array.from({ length: 8 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        </section>
      )}

      {showIdle && (
        <div className="status-section glass-panel" aria-label="Start searching">
          <div className="status-icon idle" aria-hidden="true">✦</div>
          <h2 className="status-title">Start exploring</h2>
          <p className="status-body">
            Tap a chip or type a brand, category, or product name. Results land on elevated glass cards.
          </p>
        </div>
      )}

      {showEmpty && (
        <div className="status-section glass-panel" aria-live="polite" aria-label="No results found">
          <div className="status-icon empty" aria-hidden="true">○</div>
          <h2 className="status-title">No results found</h2>
          <p className="status-body">
            Nothing matched <strong>"{query}"</strong>. Try another keyword or a suggestion chip.
          </p>
        </div>
      )}

      {showError && (
        <div className="status-section glass-panel" aria-live="polite" role="alert">
          <div className="status-icon error" aria-hidden="true">!</div>
          <h2 className="status-title">Something went wrong</h2>
          <p className="status-body error-text">{error}</p>
        </div>
      )}

      {showCards && (
        <section className="results-section" aria-label="Search results">
          <div className="results-header glass-panel">
            <p className="results-count">
              Found <strong>{visibleProducts.length}</strong> of <strong>{products.length}</strong> for{" "}
              <strong>"{query}"</strong>
            </p>
            <div className="sort-row" role="group" aria-label="Sort results">
              {[
                { id: "relevance", label: "Relevance" },
                { id: "rating", label: "Top rated" },
                { id: "price-asc", label: "Price ↑" },
                { id: "price-desc", label: "Price ↓" },
              ].map((option) => (
                <button
                  key={option.id}
                  type="button"
                  className={`md-chip ${sortBy === option.id ? "is-selected" : ""}`}
                  onClick={() => setSortBy(option.id)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          {categories.length > 1 && (
            <div className="filter-row" role="tablist" aria-label="Filter by category">
              <button
                type="button"
                className={`md-chip ${categoryFilter === "all" ? "is-selected" : ""}`}
                onClick={() => setCategoryFilter("all")}
              >
                All
              </button>
              {categories.map((category) => (
                <button
                  key={category}
                  type="button"
                  className={`md-chip ${categoryFilter === category ? "is-selected" : ""}`}
                  onClick={() => setCategoryFilter(category)}
                >
                  {category}
                </button>
              ))}
            </div>
          )}

          <div className="product-grid" role="list" aria-label={`${visibleProducts.length} products found`}>
            {visibleProducts.map((product) => {
              const saved = isFavorite(product.id);
              return (
                <article
                  key={product.id}
                  className="product-card glass-panel"
                  role="listitem"
                  tabIndex={0}
                  onClick={() => navigate(`/products/${product.id}`)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      navigate(`/products/${product.id}`);
                    }
                  }}
                  aria-label={`${product.title} by ${product.brand}, $${product.price}, rated ${product.rating}`}
                >
                  <div className="product-image-wrapper">
                    {product.category && (
                      <span className="product-category-badge">{product.category}</span>
                    )}
                    <button
                      type="button"
                      className={`favorite-btn ${saved ? "is-saved" : ""}`}
                      aria-label={saved ? "Remove from saved" : "Save product"}
                      aria-pressed={saved}
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleFavorite(product.id);
                      }}
                    >
                      <IconHeart filled={saved} />
                    </button>
                    <img
                      src={product.thumbnail}
                      alt={product.title}
                      loading="lazy"
                      onError={(e) => {
                        e.target.style.display = "none";
                      }}
                    />
                  </div>

                  <div className="product-info">
                    {product.brand && <p className="product-brand">{product.brand}</p>}
                    <h3 className="product-title">{product.title}</h3>
                    <div className="product-footer">
                      <span className="product-price">${product.price}</span>
                      <span className="product-rating">
                        <IconStar />
                        {product.rating}
                      </span>
                    </div>
                  </div>

                  <div className="product-arrow" aria-hidden="true">
                    <IconArrow />
                  </div>
                </article>
              );
            })}
          </div>
        </section>
      )}

      <footer className="search-footer">
        <p>ShopSearch © {new Date().getFullYear()} — Material glass discovery</p>
      </footer>
    </div>
  );
}

export default SearchPage;
