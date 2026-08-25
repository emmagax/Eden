import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "@fontsource-variable/doto/wght.css";
import "@fontsource-variable/suse-mono/wght.css";
import "./index.css";
import App from "./App.tsx";
import { BrowserRouter } from "react-router";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
