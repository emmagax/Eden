import { setupServer } from "msw/node";
import { http, HttpResponse } from "msw";

export const server = setupServer(
  http.get("*/auth/csrf", () => HttpResponse.json({ headerName: "X-CSRF-TOKEN", token: "test-csrf" })),
);
