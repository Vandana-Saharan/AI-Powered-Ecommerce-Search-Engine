import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import AppBar from "../components/AppBar";
import { useTheme } from "../theme/ThemeContext";
import "../App.css";

function IconStar({ filled = true }) {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

function IconPackage() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="m7.5 4.27 9 5.15M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
      <path d="m3.3 7 8.7 5 8.7-5M12 22V12" />
    </svg>
  );
}

function IconShield() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  );
}

function IconTruck() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M5 17H3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v3" />
      <rect x="9" y="11" width="14" height="10" rx="2" />
      <circle cx="12" cy="21" r="1" />
      <circle cx="20" cy="21" r="1" />
    </svg>
  );
}

function IconRotate() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
      <path d="M3 3v5h5" />
    </svg>
  );
}

function IconTag() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M12 2H2v10l9.29 9.29a1 1 0 0 0 1.42 0l8.28-8.28a1 1 0 0 0 0-1.42z" />
      <path d="M7 7h.01" />
    </svg>
  );
}

function IconHeart({ filled }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  );
}

function StarRating({ rating, count }) {
  const stars = Array.from({ length: 5 }, (_, i) => i + 1);
  return (
    <div className="star-rating" aria-label={`Rating: ${rating} out of 5`}>
      <div className="stars">
        {stars.map((s) => (
          <span key={s} className={s <= Math.round(rating) ? "star filled" : "star"}>
            <IconStar filled={s <= Math.round(rating)} />
          </span>
        ))}
      </div>
      <span className="rating-number">{rating?.toFixed(1)}</span>
      {count != null && <span className="rating-count">({count} reviews)</span>}
    </div>
  );
}

function ImageGallery({ images = [], thumbnail, title }) {
  const allImages = images.length > 0 ? images : [thumbnail];
  const [active, setActive] = useState(0);

  return (
    <div className="gallery">
      <div className="gallery-main glass-panel">
        <img
          src={allImages[active]}
          alt={title}
          className="gallery-hero-img"
          onError={(e) => {
            e.target.src = thumbnail;
          }}
        />
      </div>
      {allImages.length > 1 && (
        <div className="gallery-thumbs" role="list" aria-label="Product images">
          {allImages.map((img, i) => (
            <button
              key={i}
              className={`gallery-thumb glass-panel ${i === active ? "active" : ""}`}
              onClick={() => setActive(i)}
              aria-label={`View image ${i + 1}`}
              aria-pressed={i === active}
            >
              <img
                src={img}
                alt={`${title} view ${i + 1}`}
                onError={(e) => {
                  e.target.src = thumbnail;
                }}
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function ReviewCard({ review }) {
  const date = new Date(review.date).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
  const initials = review.reviewerName
    .split(" ")
    .map((n) => n[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="review-card glass-panel">
      <div className="review-header">
        <div className="reviewer-avatar" aria-hidden="true">
          {initials}
        </div>
        <div className="reviewer-info">
          <p className="reviewer-name">{review.reviewerName}</p>
          <p className="review-date">{date}</p>
        </div>
        <div className="review-stars" aria-label={`Rating ${review.rating} out of 5`}>
          {Array.from({ length: 5 }, (_, i) => (
            <span key={i} className={i < review.rating ? "star filled" : "star"}>
              <IconStar filled={i < review.rating} />
            </span>
          ))}
        </div>
      </div>
      <p className="review-comment">"{review.comment}"</p>
    </div>
  );
}

function DetailsSkeleton() {
  return (
    <div className="details-skeleton" aria-hidden="true" aria-label="Loading product details">
      <div className="details-layout">
        <div className="skeleton-gallery">
          <div className="skeleton-line" style={{ height: "400px", borderRadius: "28px" }} />
          <div style={{ display: "flex", gap: "10px", marginTop: "12px" }}>
            {[1, 2, 3].map((i) => (
              <div key={i} className="skeleton-line" style={{ height: "72px", flex: 1, borderRadius: "16px" }} />
            ))}
          </div>
        </div>
        <div className="skeleton-info">
          <div className="skeleton-line sm" style={{ width: "30%", height: "12px" }} />
          <div className="skeleton-line" style={{ width: "80%", height: "36px", marginTop: "12px" }} />
          <div className="skeleton-line" style={{ width: "60%", height: "36px", marginTop: "8px" }} />
          <div className="skeleton-line" style={{ width: "40%", height: "20px", marginTop: "16px" }} />
          <div className="skeleton-line" style={{ width: "30%", height: "40px", marginTop: "20px" }} />
        </div>
      </div>
    </div>
  );
}

function ProductDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isFavorite, toggleFavorite } = useTheme();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [qty, setQty] = useState(1);

  useEffect(() => {
    const fetchProduct = async () => {
      setLoading(true);
      setError("");
      try {
        const res = await fetch(`http://localhost:8080/api/products/${id}`);
        if (!res.ok) throw new Error("Product not found");
        const data = await res.json();
        setProduct(data);
        setQty(data.minimumOrderQuantity || 1);
      } catch (err) {
        console.error("Product fetch error:", err);
        setError("Could not load product details.");
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [id]);

  const discountedPrice = product?.discountPercentage
    ? (product.price * (1 - product.discountPercentage / 100)).toFixed(2)
    : null;

  const availabilityColor = {
    "In Stock": "var(--color-success)",
    "Low Stock": "var(--color-warning)",
    "Out of Stock": "var(--color-danger)",
  }[product?.availabilityStatus] ?? "var(--color-text-dim)";

  const saved = product ? isFavorite(product.id) : false;
  const unitPrice = Number(discountedPrice || product?.price || 0);

  return (
    <div className="details-page">
      <AppBar showBack />

      {product && (
        <nav className="breadcrumb glass-chip" aria-label="Breadcrumb">
          <button type="button" onClick={() => navigate("/")}>Search</button>
          <span className="breadcrumb-sep" aria-hidden="true">›</span>
          <span className="breadcrumb-category">{product.category}</span>
          <span className="breadcrumb-sep" aria-hidden="true">›</span>
          <span className="breadcrumb-current">{product.title}</span>
        </nav>
      )}

      {loading && (
        <main className="details-content">
          <DetailsSkeleton />
        </main>
      )}

      {!loading && error && (
        <main className="details-content">
          <div className="status-section glass-panel">
            <div className="status-icon error" aria-hidden="true">!</div>
            <h2 className="status-title">Product not found</h2>
            <p className="status-body error-text">{error}</p>
            <button className="search-btn" style={{ marginTop: "24px" }} onClick={() => navigate("/")}>
              Back to Search
            </button>
          </div>
        </main>
      )}

      {!loading && !error && product && (
        <main className="details-content" id="product-detail-main">
          <div className="details-layout">
            <section className="details-gallery-col" aria-label="Product images">
              <ImageGallery images={product.images || []} thumbnail={product.thumbnail} title={product.title} />
            </section>

            <section className="details-info-col glass-panel details-info-sheet" aria-label="Product information">
              <div className="details-meta-row">
                {product.brand && <span className="details-brand">{product.brand}</span>}
                {product.category && <span className="details-category-pill">{product.category}</span>}
              </div>

              <h1 className="details-title">{product.title}</h1>

              {product.rating != null && (
                <div className="details-rating-row">
                  <StarRating rating={product.rating} count={product.reviews?.length} />
                </div>
              )}

              <div className="details-price-block">
                {discountedPrice ? (
                  <>
                    <span className="details-price">${discountedPrice}</span>
                    <span className="details-price-original">${product.price?.toFixed(2)}</span>
                    <span className="details-discount-badge">-{Math.round(product.discountPercentage)}% OFF</span>
                  </>
                ) : (
                  <span className="details-price">${product.price?.toFixed(2)}</span>
                )}
              </div>

              <div className="details-availability" style={{ color: availabilityColor }}>
                <span className="availability-dot" style={{ background: availabilityColor }} aria-hidden="true" />
                {product.availabilityStatus ?? "Unknown"}
                {product.stock != null && <span className="stock-count">· {product.stock} in stock</span>}
              </div>

              {product.description && <p className="details-description">{product.description}</p>}

              {product.tags?.length > 0 && (
                <div className="details-tags" aria-label="Tags">
                  <IconTag />
                  {product.tags.map((tag) => (
                    <span key={tag} className="tag-pill">{tag}</span>
                  ))}
                </div>
              )}

              <div className="purchase-row">
                <div className="qty-stepper" aria-label="Quantity">
                  <button type="button" onClick={() => setQty((n) => Math.max(product.minimumOrderQuantity || 1, n - 1))} aria-label="Decrease quantity">−</button>
                  <span>{qty}</span>
                  <button type="button" onClick={() => setQty((n) => n + 1)} aria-label="Increase quantity">+</button>
                </div>
                <button type="button" className="search-btn purchase-cta">
                  Add · ${(unitPrice * qty).toFixed(2)}
                </button>
                <button
                  type="button"
                  className={`favorite-btn details-fav ${saved ? "is-saved" : ""}`}
                  aria-label={saved ? "Remove from saved" : "Save product"}
                  aria-pressed={saved}
                  onClick={() => toggleFavorite(product.id)}
                >
                  <IconHeart filled={saved} />
                </button>
              </div>

              <div className="details-info-cards">
                {product.shippingInformation && (
                  <div className="info-card">
                    <span className="info-card-icon"><IconTruck /></span>
                    <div>
                      <p className="info-card-label">Shipping</p>
                      <p className="info-card-value">{product.shippingInformation}</p>
                    </div>
                  </div>
                )}
                {product.warrantyInformation && (
                  <div className="info-card">
                    <span className="info-card-icon"><IconShield /></span>
                    <div>
                      <p className="info-card-label">Warranty</p>
                      <p className="info-card-value">{product.warrantyInformation}</p>
                    </div>
                  </div>
                )}
                {product.returnPolicy && (
                  <div className="info-card">
                    <span className="info-card-icon"><IconRotate /></span>
                    <div>
                      <p className="info-card-label">Returns</p>
                      <p className="info-card-value">{product.returnPolicy}</p>
                    </div>
                  </div>
                )}
              </div>

              <div className="details-specs">
                {product.sku && (
                  <div className="spec-row">
                    <span className="spec-label">SKU</span>
                    <code className="spec-value">{product.sku}</code>
                  </div>
                )}
                {product.weight != null && (
                  <div className="spec-row">
                    <span className="spec-label">Weight</span>
                    <span className="spec-value">{product.weight} g</span>
                  </div>
                )}
                {product.minimumOrderQuantity != null && (
                  <div className="spec-row">
                    <span className="spec-label">Min. Order</span>
                    <span className="spec-value">{product.minimumOrderQuantity} unit{product.minimumOrderQuantity !== 1 ? "s" : ""}</span>
                  </div>
                )}
              </div>
            </section>
          </div>

          {product.reviews?.length > 0 && (
            <section className="details-reviews" aria-label="Customer reviews">
              <div className="reviews-header">
                <h2 className="reviews-title">Customer reviews</h2>
                <span className="reviews-badge glass-chip">
                  <IconPackage />
                  {product.reviews.length} verified review{product.reviews.length !== 1 ? "s" : ""}
                </span>
              </div>
              <div className="reviews-grid">
                {product.reviews.map((review, i) => (
                  <ReviewCard key={i} review={review} />
                ))}
              </div>
            </section>
          )}
        </main>
      )}

      <footer className="search-footer">
        <p>ShopSearch © {new Date().getFullYear()} — Material glass discovery</p>
      </footer>
    </div>
  );
}

export default ProductDetailsPage;
