import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, it } from "vitest";
import LoginPage from "./LoginPage";

describe('LoginPage', () => {
  it("shows the sign-in form with the submission disabled initially", () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: "Sign in" }),
    ).toBeInTheDocument();

    expect(
      screen.getByRole("button", { name: "Sign in" }),
    ).toBeDisabled();
  });
});

