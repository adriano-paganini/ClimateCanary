/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React, { useState } from "react";
import { Button } from "primereact/button";
import { useNavigate } from "react-router-dom";
import { useUser } from "../Contexts/AuthenticatedUserContext";
import { ROUTES } from "../utilities/routes.paths";

import logo from "../../public/512x512.png"

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const { login } = useUser();
    const navigate = useNavigate();

    const handleLogin = async (e: any) => {
        e.preventDefault();
        if (loading) return;

        setError(null);
        setLoading(true);

        try {
            await login({ username, password });
            navigate(ROUTES.PROFILE, { replace: true });
        } catch (err: any) {
            const status = err?.response?.status as number | undefined;
            if (status === 401 || status === 403) {
                setError("Wrong username or password");
            } else if (status === 500) {
                setError("Server error");
            } else if (status === undefined) {
                setError("No connection to server. Try again later");
            } else {
                setError("Login failed. Please try again.");
            }
            console.error("Login failed:", err);
        } finally {
            setPassword("");
            setLoading(false);
        }
    };

    return (
        <>
            <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500&family=Playfair+Display:wght@500&display=swap');

        .login-wrap {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(145deg, #e8f4f0 0%, #d4ecf7 40%, #c8e6d4 100%);
          position: relative;
          overflow: hidden;
          font-family: 'DM Sans', sans-serif;
        }

        .login-bg-logo {
          position: absolute;
          width: min(750px, 95vw); 
          height: min(750px, 95vw);
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          opacity: 0.13;
          pointer-events: none;
          user-select: none;
          -webkit-mask-image: radial-gradient(circle, rgba(0,0,0,1) 55%, rgba(0,0,0,0) 80%);
          mask-image: radial-gradient(circle, rgba(0,0,0,1) 55%, rgba(0,0,0,0) 80%);
        }

        .login-bg-logo img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }

        .login-card {
          position: relative;
          z-index: 2;
          background: rgba(255, 255, 255, 0.72);
          backdrop-filter: blur(18px);
          -webkit-backdrop-filter: blur(18px);
          border: 0.5px solid rgba(255, 255, 255, 0.9);
          border-radius: 20px;
          padding: 2.5rem 2.75rem 2.25rem;
          width: 340px;
          box-shadow: 0 8px 40px rgba(30, 110, 80, 0.10), 0 2px 8px rgba(30, 80, 120, 0.07);
          animation: cardIn 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
          transition: transform 0.25s ease, box-shadow 0.25s ease;
          will-change: transform;
        }
        
        .login-card:hover {
          transform: scale(1.03);
          box-shadow: 0 14px 55px rgba(30, 110, 80, 0.18),
          0 6px 18px rgba(30, 80, 120, 0.12);
        }

        @keyframes cardIn {
          from { opacity: 0; transform: translateY(18px) scale(0.97); }
          to   { opacity: 1; transform: translateY(0) scale(1); }
        }
        
        @keyframes bgPulse {
          0%, 100% { transform: translate(-50%, -50%) scale(1);    opacity: 0.13; }
          50%       { transform: translate(-50%, -50%) scale(1.06); opacity: 0.18; }
        }
        
        .login-bg-logo {
          animation: bgPulse 4s ease-in-out infinite;
        }

        .login-logo-wrap {
          display: flex;
          justify-content: center;
          margin-bottom: 0.25rem;
        }

        .login-logo-wrap img {
          width: 52px;
          height: 52px;
          object-fit: contain;
        }

        .login-title {
          font-family: 'Playfair Display', serif;
          font-size: 22px;
          font-weight: 500;
          color: #1a4a38;
          text-align: center;
          margin: 0.5rem 0 0.25rem;
          letter-spacing: -0.3px;
        }

        .login-subtitle {
          font-size: 13px;
          color: #5a8a72;
          text-align: center;
          margin: 0 0 1.75rem;
          font-weight: 300;
          letter-spacing: 0.01em;
        }

        .login-field {
          margin-bottom: 1rem;
        }

        .login-field-label {
          font-size: 11px;
          font-weight: 500;
          color: #4a7a60;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          display: block;
          margin-bottom: 6px;
        }

        .login-input {
          width: 100%;
          height: 42px;
          border-radius: 10px;
          border: 1px solid rgba(30, 130, 90, 0.22);
          background: rgba(255, 255, 255, 0.8);
          padding: 0 12px;
          font-size: 14px;
          font-family: 'DM Sans', sans-serif;
          color: #1a3a28;
          outline: none;
          box-sizing: border-box;
          transition: border-color 0.2s, box-shadow 0.2s;
        }

        .login-input:focus {
          border-color: #2a9a68;
          box-shadow: 0 0 0 3px rgba(42, 154, 104, 0.12);
          background: rgba(255, 255, 255, 0.95);
        }

        .login-input::placeholder {
          color: #aac5b8;
        }

        .login-pw-wrap {
          position: relative;
        }

        .login-pw-wrap .login-input {
          padding-right: 40px;
        }

        .login-pw-toggle {
          position: absolute;
          right: 12px;
          top: 50%;
          transform: translateY(-50%);
          background: none;
          border: none;
          cursor: pointer;
          color: #7aaa90;
          font-size: 16px;
          padding: 0;
          display: flex;
          align-items: center;
          line-height: 1;
        }

        .login-pw-toggle:hover {
          color: #2a9a68;
        }

        .login-submit-btn {
          width: 100%;
          height: 44px;
          border-radius: 10px !important;
          border: none !important;
          background: linear-gradient(135deg, #1e9068 0%, #1a7ab0 100%) !important;
          color: white !important;
          font-family: 'DM Sans', sans-serif !important;
          font-size: 14px !important;
          font-weight: 500 !important;
          cursor: pointer;
          margin-top: 0.25rem;
          letter-spacing: 0.02em;
          transition: opacity 0.2s, transform 0.1s !important;
          justify-content: center;
        }

        .login-submit-btn:hover:not(:disabled) {
          opacity: 0.92 !important;
          background: linear-gradient(135deg, #1e9068 0%, #1a7ab0 100%) !important;
        }

        .login-submit-btn:active:not(:disabled) {
          transform: scale(0.985) !important;
        }

        .login-error {
          font-size: 12.5px;
          color: #c0392b;
          background: rgba(220, 80, 60, 0.07);
          border-radius: 8px;
          padding: 8px 12px;
          margin-top: 0.75rem;
          text-align: center;
        }

        .login-spinner-wrap {
          display: flex;
          justify-content: center;
          margin-top: 0.75rem;
        }
        
        .login-user-icon {
          font-size: 42px;
          color: #2a9a68;
          background: rgba(42, 154, 104, 0.12);
          padding: 14px;
          border-radius: 50%;
        }
        
        .app-name-label {
          font-size: 11px;
          font-weight: 500;n
          color: #2a9a68;
          text-transform: uppercase;
          letter-spacing: 0.13em;
          text-align: center;
          margin: 0.9rem 0 0.1rem;
        }
      `}</style>

            <div className="login-wrap">
                <div className="login-bg-logo" aria-hidden="true">
                    <img src={logo} alt="" />
                </div>

                <div className="login-card">
                    <div className="login-logo-wrap">
                        <i className="pi pi-user login-user-icon" />
                    </div>

                    <p className="app-name-label">Climate Canary</p>
                    <h2 className="login-title">Welcome back</h2>
                    <p className="login-subtitle">Sign in to your account</p>

                    <form onSubmit={handleLogin}>
                        <div className="login-field">
                            <label className="login-field-label" htmlFor="username">
                                Username
                            </label>
                            <input
                                id="username"
                                className="login-input"
                                type="text"
                                placeholder="Enter your username"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                autoComplete="off"
                            />
                        </div>

                        <div className="login-field">
                            <label className="login-field-label" htmlFor="password">
                                Password
                            </label>
                            <div className="login-pw-wrap">
                                <input
                                    id="password"
                                    className="login-input"
                                    type={showPassword ? "text" : "password"}
                                    placeholder="Enter your password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    autoComplete="off"
                                />
                                <button
                                    type="button"
                                    className="login-pw-toggle"
                                    onClick={() => setShowPassword((v) => !v)}
                                    aria-label={showPassword ? "Hide password" : "Show password"}
                                >
                                    <i className={showPassword ? "pi pi-eye-slash" : "pi pi-eye"} />
                                </button>
                            </div>
                        </div>

                        <Button
                            type="submit"
                            label="Sign in"
                            loading={loading}
                            className="login-submit-btn"
                        />
                    </form>

                    {error && <p className="login-error">{error}</p>}
                </div>
            </div>
        </>
    );
};

export default Login;
