import { useState, type SubmitEvent } from "react";
import { Link } from "react-router";
function LoginPage() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const canSubmit = identifier.trim().length > 0 && password.length > 0;

  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canSubmit) {
      return;
    }
  }

  return (
    <main>
      <section aria-labelledby="login-title">
        <header>
          <div className="logo" role="img" aria-label="Eden">
            <span aria-hidden="true">
              {"{*"}
              <span className="logo-tight">{"}"}</span>
            </span>
          </div>
          <h1 id="login-title" className="login-title">
            Sign in
          </h1>
        </header>

        <form onSubmit={handleSubmit} className="login-form">
          <div>
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
          </div>

          <div>
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
          </div>

          <button
            className="sign-in-btn"
            type="submit"
            disabled={!canSubmit}
            aria-label="Sign in"
          >
            {">"}
          </button>
        </form>

        <p>
          Don&apos;t have an account? <br />
          <Link id="register-link" to="/register">
            Create an account
          </Link>
        </p>
      </section>
    </main>
  );
}

export default LoginPage;
