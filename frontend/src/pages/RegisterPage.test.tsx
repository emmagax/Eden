import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router";
import { describe, expect, it } from "vitest";
import { server } from "../test/server";
import RegisterPage from "./RegisterPage";

describe("RegisterPage", () => {
  it("submits registration details to the backend and shows success", async () => {
    server.use(
      http.post("*/auth/register", async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({
          email: "emma@example.com",
          username: "emma",
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
        <RegisterPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText("Email"), "emma@example.com");
    await userEvent.type(screen.getByLabelText("Username"), "emma");
    await userEvent.type(screen.getByLabelText("Password"), "Password123!");
    await userEvent.type(
      screen.getByLabelText("Confirm password"),
      "Password123!",
    );
    await userEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Account created for emma. You can now sign in.",
    );
  });

  it("shows backend registration errors", async () => {
    server.use(
      http.post("*/auth/register", () => {
        return HttpResponse.json(
          { message: "An account with this email already exists" },
          { status: 409 },
        );
      }),
    );

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText("Email"), "emma@example.com");
    await userEvent.type(screen.getByLabelText("Username"), "emma");
    await userEvent.type(screen.getByLabelText("Password"), "Password123!");
    await userEvent.type(
      screen.getByLabelText("Confirm password"),
      "Password123!",
    );
    await userEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "An account with this email already exists",
    );
  });
});
