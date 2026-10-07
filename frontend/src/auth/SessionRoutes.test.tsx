import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import App from "../App";
import { server } from "../test/server";

describe("session routes", () => {
  it("restores a cookie session and signs out", async () => {
    server.use(
      http.get("*/auth/me", () => HttpResponse.json({ id: 1, email: "emma@example.com", username: "emma" })),
      http.post("*/auth/logout", ({ request }) => {
        expect(request.headers.get("X-CSRF-TOKEN")).toBe("test-csrf");
        return new HttpResponse(null, { status: 204 });
      }),
    );
    render(<MemoryRouter initialEntries={["/account"]}><App /></MemoryRouter>);
    expect(await screen.findByText("Signed in as emma.")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Sign out" }));
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });
  it("redirects anonymous visitors away from a protected route", async () => {
    server.use(http.get("*/auth/me", () => new HttpResponse(null, { status: 401 })));
    render(<MemoryRouter initialEntries={["/account"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });
  it("shows a retryable error instead of treating a server failure as signed out", async () => {
    server.use(http.get("*/auth/me", () => new HttpResponse(null, { status: 503 })));
    render(<MemoryRouter initialEntries={["/account"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to check your session");
    server.use(http.get("*/auth/me", () => new HttpResponse(null, { status: 401 })));
    await userEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });
  it("moves to the account page after sign-in", async () => {
    server.use(
      http.get("*/auth/me", () => new HttpResponse(null, { status: 401 })),
      http.post("*/auth/login", () => HttpResponse.json({ id: 1, email: "emma@example.com", username: "emma" })),
    );
    render(<MemoryRouter initialEntries={["/login"]}><App /></MemoryRouter>);
    await screen.findByRole("heading", { name: "Sign in" });
    await userEvent.type(screen.getByLabelText("Email or username"), "emma");
    await userEvent.type(screen.getByLabelText("Password"), "Password123!");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByRole("heading", { name: "Your account" })).toBeInTheDocument();
  });
});
