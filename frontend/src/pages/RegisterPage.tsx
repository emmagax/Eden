import { Link } from "react-router";

function RegisterPage() {
  return (
    <main>
      <section aria-labelledby="register-title">
        <h1 id="register-title">Create an account</h1>

        <p>The registration form will go here.</p>

        <p>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </section>
    </main>
  );
}

export default RegisterPage;
