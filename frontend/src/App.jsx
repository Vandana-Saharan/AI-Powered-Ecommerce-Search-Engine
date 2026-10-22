
import {
  BrowserRouter,
  Routes,
  Route
} from "react-router-dom";

import SearchPage from "./pages/SearchPage";
import ProductDetailsPage from "./pages/ProductDetailsPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<SearchPage />}
        />

        <Route
          path="/products/:id"
          element={<ProductDetailsPage />}
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
