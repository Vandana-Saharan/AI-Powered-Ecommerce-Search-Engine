import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "./theme/ThemeProvider";
import SearchPage from "./pages/SearchPage";
import ProductDetailsPage from "./pages/ProductDetailsPage";

function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <div className="ambient-layer" aria-hidden="true">
          <span className="orb orb-a" />
          <span className="orb orb-b" />
          <span className="orb orb-c" />
        </div>
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/products/:id" element={<ProductDetailsPage />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
