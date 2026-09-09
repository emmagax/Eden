import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "./server";


describe("API test setup", () => {
  it("intercepts an API request", async () => {
    server.use(
      http.get("http://localhost/api/example", () => {
        return HttpResponse.json({ message: "Hello from the mock API" });
      }),
    );

    const response = await fetch("http://localhost/api/example");
    const body = await response.json();

    expect(response.ok).toBe(true);
    expect(body).toEqual({ message: "Hello from the mock API" });
  });
})
