/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import { useState } from "react";

import { Button } from "primereact/button";
import { FloatLabel } from "primereact/floatlabel";
import { InputText } from "primereact/inputtext";
import { Message } from "primereact/message";
import { Password } from "primereact/password";

import "../styles/Login.css";

import { useNavigate } from "react-router-dom";
import { jwtDecode, JwtPayload } from "jwt-decode";
import { useUser } from "../Contexts/AuthenticatedUserContext";
import { BEARER_TOKEN_LOCAL_STORAGE_KEY } from "../config/config";
import { UserxRole } from "../generated-skeleton-api";
import { ROUTES } from "../utilities/routes.paths";

type CustomJwtPayload = JwtPayload & { roles: string[] };

/**
 * Login component
 */

const Login = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login } = useUser();

  const navigate = useNavigate();

  const getRoleBasedRedirect = (): string => {
    const token = localStorage.getItem(BEARER_TOKEN_LOCAL_STORAGE_KEY);
    if (!token) return ROUTES.HOME;
    try {
      const decoded = jwtDecode<CustomJwtPayload>(token);
      if ((decoded.roles ?? []).includes(UserxRole.ADMIN)) {
        return ROUTES.MANAGE_USERS;
      }
    } catch {}
    return ROUTES.HOME; //fallback to home on decode error
  };

  /**
   * Handle login event and send login request to the server
   * @param e Form event
   *
   * Sets error message if login fails
   * Redirects to role-specific page if login is successful
   *
   */
  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (loading) {
      return;
    }

    setError(null);
    setLoading(true);

    try {
      await login({ username, password });
      navigate(getRoleBasedRedirect(), { replace: true });
    } catch (err: any) {
      const status = err?.response?.status as number | undefined;
      if (status === 401 || status === 403) {
        setError(
          "Benutzername oder Passwort ist falsch. Bitte versuche es erneut.",
        );
      } else if (status === 500) {
        setError("Serverfehler. Bitte kontaktiere den Administrator.");
      } else if (status === undefined) {
        setError(
          "Keine Verbindung zum Server. Bitte versuche es später erneut.",
        );
      } else {
        setError("Login fehlgeschlagen. Bitte versuche es erneut.");
      }
      console.error("Login failed:", err);
    } finally {
      setPassword("");
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Login</h2>
        <form onSubmit={handleLogin}>
          <FloatLabel style={{ marginTop: 50 }}>
            <InputText
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoComplete="off"
              className="input-field"
              disabled={loading}
            />
            <label htmlFor="username">Username:</label>
          </FloatLabel>

          <FloatLabel style={{ marginTop: 25 }}>
            <Password
              inputId="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              feedback={false}
              autoComplete="off"
              className="input-field"
              disabled={loading}
            />
            <label htmlFor="password">Password:</label>
          </FloatLabel>
          <Button
            type="submit"
            label="Login"
            className="loginButton"
            loading={loading}
            disabled={loading}
          />
        </form>
        {error && (
          <Message
            severity="error"
            text={error}
            style={{ marginTop: 25, width: "100%" }}
          />
        )}
      </div>
    </div>
  );
};

export default Login;
