import { useState, type SubmitEvent } from "react";
import { Link } from "react-router";
import { register, type AuthUser } from "../api/auth";
import "../index.css";

function RegisterPage() {
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [createdUser, setCreatedUser] = useState<AuthUser | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordsMatch = password === confirmPassword;

  const showPasswordMismatch = confirmPassword.length > 0 && !passwordsMatch;

  const canSubmit =
    email.trim().length > 0 &&
    username.trim().length >= 3 &&
    password.length >= 8 &&
    confirmPassword.length > 0 &&
    passwordsMatch &&
    !isSubmitting;

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canSubmit) {
      return;
    }

    setErrorMessage("");
    setCreatedUser(null);
    setIsSubmitting(true);

    try {
      const user = await register({
        email: email.trim(),
        username: username.trim(),
        password,
      });
      setCreatedUser(user);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to create account.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main>
      <section className="header">
        <div className="text-logo" aria-label="Eden">
          <h1>EDEN</h1>
        </div>
      </section>
      <section className="auth-panel" aria-labelledby="register-title">
        <header>
          <h1 id="register-title" className="auth-title">
            Register
          </h1>
        </header>

        <form onSubmit={handleSubmit} className="auth-form">
          <label className="visually-hidden" htmlFor="register-email">
            Email
          </label>
          <br />
          <input
            id="register-email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder="e-mail"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />

          <label className="visually-hidden" htmlFor="register-username">
            Username
          </label>
          <br />
          <input
            id="register-username"
            name="username"
            type="text"
            autoComplete="username"
            placeholder="username"
            minLength={3}
            maxLength={30}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />

          <label className="visually-hidden" htmlFor="register-password">
            Password
          </label>
          <br />

          <input
            id="register-password"
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder="password"
            minLength={8}
            maxLength={72}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />

          <div className="confirm-password-field">
            <label
              className="visually-hidden"
              htmlFor="register-confirm-password"
            >
              Confirm password
            </label>

            <input
              id="register-confirm-password"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="confirm password"
              minLength={8}
              maxLength={72}
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              aria-invalid={showPasswordMismatch}
              aria-describedby={
                showPasswordMismatch ? "confirm-password-error" : undefined
              }
              required
            />

            {showPasswordMismatch && (
              <p
                id="confirm-password-error"
                className="field-error"
                role="alert"
              >
                Passwords do not match.
              </p>
            )}
          </div>

          {errorMessage && (
            <p className="form-message error-message" role="alert">
              {errorMessage}
            </p>
          )}

          {createdUser && (
            <p className="form-message success-message" role="status">
              Account created for {createdUser.username}. You can now sign in.
            </p>
          )}

          <button
            className="submit-button"
            type="submit"
            disabled={!canSubmit}
            aria-label="Create account"
          >
            {isSubmitting ? "..." : ">"}
          </button>
        </form>

        <p className="footer-text">
          Already have an account? <br />
          <Link id="auth-link" to="/login">
            Sign in
          </Link>
        </p>
      </section>
    </main>
  );
}

export default RegisterPage;
