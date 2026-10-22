
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../App.css";

function SearchPage() {
  const [query, setQuery] = useState("");
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const navigate = useNavigate();

  const searchProducts = async () => {
    if (!query.trim()) return;

    setLoading(true);
    setError("");

    try {
      const response = await fetch(
        `http://localhost:8080/api/products/search?q=${encodeURIComponent(query.trim())}`
      );

      if (!response.ok) {
        throw new Error("Failed to fetch products");
      }

const data = await response.json();

console.log("Search Query:", query);
console.log("API Response:", data);
console.log("Number of Products:", data.length);

setProducts(data);  


    } 
catch (err) {
  console.error("Search API Error:", err);
  setError("Something went wrong while searching.");
  setProducts([]);
}
 finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1>E-Commerce Product Search</h1>

      {/* Search Bar */}
      <input
        type="text"
        placeholder="Search products..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            searchProducts();
          }
        }}
      />

      <button onClick={searchProducts}>
        Search
      </button>

      {loading && <p>Searching...</p>}
      {error && <p>{error}</p>}

      {!loading && products.length === 0 && !error && (
        <p>No products to display. Try searching!</p>
      )}

      {/* Product Cards */}
      <div className="product-grid">
        {products.map((product) => (
          <div
            key={product.id}
            className="product-card"
            role="link"
            tabIndex={0}
            onClick={() => navigate(`/products/${product.id}`)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                navigate(`/products/${product.id}`);
              }
            }}
          >
            <img
              src={product.thumbnail}
              alt={product.title}
            />

            <div className="product-info">
              <p className="brand">{product.brand}</p>
              <h3>{product.title}</h3>
              <p className="category">{product.category}</p>

              <div className="product-bottom">
                <span className="price">
                  ${product.price}
                </span>
                <span className="rating">
                  ⭐ {product.rating}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default SearchPage;
