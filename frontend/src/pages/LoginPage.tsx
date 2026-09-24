import { useState, type SubmitEvent } from "react";
import { Link } from "react-router";
import { login, type AuthUser } from "../api/auth";

function LoginPage() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const canSubmit = identifier.trim().length > 0 && password.length > 0 && !isSubmitting;

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canSubmit) {
      return;
    }

    setErrorMessage("");
    setCurrentUser(null);
    setIsSubmitting(true);

    try {
      const user = await login({
        identifier: identifier.trim(),
        password,
      });
      setCurrentUser(user);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to sign in.");
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
      <section className="auth-panel" aria-labelledby="login-title">
        <header>
          {/*  */}
          <h1 id="login-title" className="auth-title">
            Sign in
          </h1>
        </header>

        <form onSubmit={handleSubmit} className="auth-form">
          <label className="visually-hidden" htmlFor="identifier">
            Email or username
          </label>
          <br />
          <input
            id="identifier"
            name="identifier"
            type="text"
            autoComplete="username"
            required
            placeholder="e-mail or username"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
          />

          <label className="visually-hidden" htmlFor="password">
            Password
          </label>
          <br />
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            placeholder="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <br />
          <div className="forgot-password">
            <Link id="forgot-password-link" to="/forgot-password">
              Forgot password?
            </Link>
          </div>

          {errorMessage && (
            <p className="form-message error-message" role="alert">
              {errorMessage}
            </p>
          )}

          {currentUser && (
            <p className="form-message success-message" role="status">
              Signed in as {currentUser.username}.
            </p>
          )}

          <button
            className="submit-button"
            type="submit"
            disabled={!canSubmit}
            aria-label="Sign in"
          >
            {isSubmitting ? "..." : ">"}
          </button>
        </form>

        <p className="footer-text">
          Don&apos;t have an account? <br />
          <Link id="auth-link" to="/register">
            Create an account
          </Link>
        </p>
      </section>
    </main>
  );
}

export default LoginPage;
