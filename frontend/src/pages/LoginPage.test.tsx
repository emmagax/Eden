import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router";
import { describe, expect, it } from "vitest";
import { server } from "../test/server";
import LoginPage from "./LoginPage";

describe("LoginPage", () => {
  it("shows the sign-in form with the submission disabled initially", () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: "Sign in" }),
    ).toBeInTheDocument();

    expect(screen.getByRole("button", { name: "Sign in" })).toBeDisabled();
  });

  it("submits credentials to the backend and shows the signed-in user", async () => {
    server.use(
      http.post("*/auth/login", async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({
          identifier: "emma",
          password: "Password123!",
        });

        return HttpResponse.json({
          id: 1,
          email: "emma@example.com",
          username: "emma",
        });
      }),
    );

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText("Email or username"), "emma");
    await userEvent.type(screen.getByLabelText("Password"), "Password123!");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Signed in as emma.",
    );
  });

  it("shows backend login errors", async () => {
    server.use(
      http.post("*/auth/login", () => {
        return HttpResponse.json(
          { message: "Invalid credentials" },
          { status: 401 },
        );
      }),
    );

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText("Email or username"), "emma");
    await userEvent.type(screen.getByLabelText("Password"), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Invalid credentials",
    );
  });
});
