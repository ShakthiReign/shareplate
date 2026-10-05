import React, { useState, useEffect } from "react";
import ReactDOM from "react-dom/client";
import "./style.css";

const API = import.meta.env.VITE_API_URL || "https://shareplate-backend.onrender.com/api";

function checkPasswordRules(pw) {
    return {
        length: pw.length >= 8 && pw.length <= 72,
        upper: /[A-Z]/.test(pw),
        lower: /[a-z]/.test(pw),
        digit: /[0-9]/.test(pw),
        special: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pw),
    };
}

function isPasswordStrong(pw) {
    const rules = checkPasswordRules(pw);
    return rules.length && rules.upper && rules.lower && rules.digit && rules.special;
}

function PasswordRequirements({ password }) {
    const rules = checkPasswordRules(password);
    return (
        <div className="password-checklist">
            <span className="checklist-title">PASSWORD MUST CONTAIN:</span>
            <ul>
                <li className={rules.length ? "valid" : ""}>
                    <i className="check-icon">{rules.length ? "\u2713" : ""}</i> 8 to 72 characters
                </li>
                <li className={rules.upper ? "valid" : ""}>
                    <i className="check-icon">{rules.upper ? "\u2713" : ""}</i> At least 1 uppercase letter (A-Z)
                </li>
                <li className={rules.lower ? "valid" : ""}>
                    <i className="check-icon">{rules.lower ? "\u2713" : ""}</i> At least 1 lowercase letter (a-z)
                </li>
                <li className={rules.digit ? "valid" : ""}>
                    <i className="check-icon">{rules.digit ? "\u2713" : ""}</i> At least 1 digit (0-9)
                </li>
                <li className={rules.special ? "valid" : ""}>
                    <i className="check-icon">{rules.special ? "\u2713" : ""}</i> At least 1 special character (!@#$%...)
                </li>
            </ul>
        </div>
    );
}

function App() {
    const [token, setToken] = useState(localStorage.getItem("token") || "");
    const [user, setUser] = useState(() => {
        try {
            return JSON.parse(localStorage.getItem("user") || "null");
        } catch {
            return null;
        }
    });

    const [theme, setTheme] = useState(localStorage.getItem("theme") || "dark");
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [showProfileModal, setShowProfileModal] = useState(false);
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [showAdminModal, setShowAdminModal] = useState(false);

    // Auth input states
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loginMessage, setLoginMessage] = useState("");
    const [loginLoading, setLoginLoading] = useState(false);

    // Signup states
    const [showSignup, setShowSignup] = useState(false);
    const [signupName, setSignupName] = useState("");
    const [signupEmail, setSignupEmail] = useState("");
    const [signupPhone, setSignupPhone] = useState("");
    const [signupPassword, setSignupPassword] = useState("");
    const [signupConfirmPassword, setSignupConfirmPassword] = useState("");
    const [signupRole, setSignupRole] = useState("DONOR");
    const [signupMessage, setSignupMessage] = useState("");
    const [signupLoading, setSignupLoading] = useState(false);

    // Verification states
    const [showVerification, setShowVerification] = useState(false);
    const [verificationEmail, setVerificationEmail] = useState("");
    const [verificationCode, setVerificationCode] = useState(["", "", "", "", "", ""]);
    const [verificationMessage, setVerificationMessage] = useState("");
    const [verificationLoading, setVerificationLoading] = useState(false);

    // Password recovery states
    const [showForgotPassword, setShowForgotPassword] = useState(false);
    const [forgotEmail, setForgotEmail] = useState("");
    const [forgotMessage, setForgotMessage] = useState("");
    const [forgotLoading, setForgotLoading] = useState(false);

    const [showResetPassword, setShowResetPassword] = useState(false);
    const [resetToken, setResetToken] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmNewPassword, setConfirmNewPassword] = useState("");
    const [resetMessage, setResetMessage] = useState("");
    const [resetLoading, setResetLoading] = useState(false);

    // Operational listings and tasks states
    const [listings, setListings] = useState([]);
    const [tasks, setTasks] = useState([]);
    const [availableVolunteers, setAvailableVolunteers] = useState([]);
    const [selectedVolunteer, setSelectedVolunteer] = useState({});
    const [isOnline, setIsOnline] = useState(false);
    const [locationStatus, setLocationStatus] = useState("");
    const [pickupCodeInput, setPickupCodeInput] = useState({});
    const [searchQuery, setSearchQuery] = useState("");
    const [filterTab, setFilterTab] = useState("ALL");

    // Donor Food Creation Form State
    const [foodTitle, setFoodTitle] = useState("");
    const [foodDescription, setFoodDescription] = useState("");
    const [foodQuantity, setFoodQuantity] = useState("");
    const [foodAddress, setFoodAddress] = useState("");
    const [foodLatitude, setFoodLatitude] = useState(null);
    const [foodLongitude, setFoodLongitude] = useState(null);
    const [foodGpsStatus, setFoodGpsStatus] = useState("");

    // Admin NGO Verification State
    const [pendingNgos, setPendingNgos] = useState([]);
    const [adminMessage, setAdminMessage] = useState("");

    // Custom avatar state
    const [userAvatar, setUserAvatar] = useState(localStorage.getItem("user_avatar") || "");

    useEffect(() => {
        document.documentElement.setAttribute("data-theme", theme);
        localStorage.setItem("theme", theme);
    }, [theme]);

    useEffect(() => {
        if (token && user) {
            fetchListings();
            if (user.role === "VOLUNTEER") {
                fetchVolunteerTasks();
            } else if (user.role === "NGO") {
                fetchNgoTasks();
                fetchOnlineVolunteers();
            } else if (user.role === "ADMIN") {
                fetchPendingNgos();
            }
        }
    }, [token, user]);

    const authHeaders = {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
    };

    const toggleTheme = () => {
        setTheme((prev) => (prev === "dark" ? "light" : "dark"));
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoginLoading(true);
        setLoginMessage("");

        try {
            const res = await fetch(`${API}/users/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email.trim(), password }),
            });

            const text = await res.text();
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch {}

            if (!res.ok) {
                throw new Error(data?.message || data?.error || text || "Invalid email or password.");
            }

            localStorage.setItem("token", data.token);
            localStorage.setItem("user", JSON.stringify(data));
            setToken(data.token);
            setUser(data);
            setIsOnline(!!data.isOnline);
        } catch (err) {
            setLoginMessage(err.message || "Login failed.");
        } finally {
            setLoginLoading(false);
        }
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setSignupMessage("");

        if (signupPassword !== signupConfirmPassword) {
            setSignupMessage("Passwords do not match.");
            return;
        }

        if (!isPasswordStrong(signupPassword)) {
            setSignupMessage("Please fulfill all password requirements below.");
            return;
        }

        setSignupLoading(true);

        try {
            const res = await fetch(`${API}/users/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: signupName.trim(),
                    email: signupEmail.trim(),
                    phone: signupPhone.trim(),
                    password: signupPassword,
                    role: signupRole,
                }),
            });

            const text = await res.text();
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch {}

            if (!res.ok) {
                throw new Error(data?.message || data?.error || text || "Could not create account.");
            }

            const regEmail = signupEmail.trim().toLowerCase();
            setVerificationEmail(regEmail);
            setVerificationCode(["", "", "", "", "", ""]);
            setVerificationMessage("A 6-digit verification code has been dispatched to your email.");
            setShowVerification(true);
            setShowSignup(false);
        } catch (err) {
            setSignupMessage(err.message || "Registration failed.");
        } finally {
            setSignupLoading(false);
        }
    };

    const handleVerify = async (e) => {
        e.preventDefault();
        const code = verificationCode.join("");
        if (code.length !== 6) {
            setVerificationMessage("Please enter all 6 digits of the code.");
            return;
        }

        setVerificationLoading(true);
        setVerificationMessage("");

        try {
            const res = await fetch(`${API}/users/verify-email`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: verificationEmail, code }),
            });

            const text = await res.text();
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch {}

            if (!res.ok) {
                throw new Error(data?.message || data?.error || text || "Verification failed.");
            }

            setShowVerification(false);
            setLoginMessage("Email verified successfully! You can now sign in.");
        } catch (err) {
            setVerificationMessage(err.message || "Verification failed.");
        } finally {
            setVerificationLoading(false);
        }
    };

    const handleForgotPassword = async (e) => {
        e.preventDefault();
        setForgotLoading(true);
        setForgotMessage("");

        try {
            const res = await fetch(`${API}/users/forgot-password`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: forgotEmail.trim() }),
            });

            const text = await res.text();
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch {}

            if (!res.ok) {
                throw new Error(data?.message || data?.error || text || "Unable to send reset instructions.");
            }

            setForgotMessage("If this account exists, password reset instructions have been sent.");
        } catch (err) {
            setForgotMessage(err.message || "Password reset request failed.");
        } finally {
            setForgotLoading(false);
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        setResetMessage("");

        if (newPassword !== confirmNewPassword) {
            setResetMessage("Passwords do not match.");
            return;
        }

        if (!isPasswordStrong(newPassword)) {
            setResetMessage("Please satisfy all password complexity rules.");
            return;
        }

        setResetLoading(true);

        try {
            const res = await fetch(`${API}/users/reset-password`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ token: resetToken.trim(), newPassword }),
            });

            const text = await res.text();
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch {}

            if (!res.ok) {
                throw new Error(data?.message || data?.error || text || "Password reset failed.");
            }

            setShowResetPassword(false);
            setLoginMessage("Password updated successfully. Please log in with your new password.");
        } catch (err) {
            setResetMessage(err.message || "Password reset failed.");
        } finally {
            setResetLoading(false);
        }
    };

    const handleLogout = () => {
        localStorage.clear();
        setToken("");
        setUser(null);
        setDrawerOpen(false);
    };

    const toggleVolunteerDuty = async () => {
        const nextStatus = !isOnline;
        setLocationStatus("Detecting live coordinates...");

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                async (pos) => {
                    await updateDutyStatus(nextStatus, pos.coords.latitude, pos.coords.longitude);
                    setLocationStatus(nextStatus ? "Duty: Online (GPS locked)" : "Duty: Offline");
                },
                async () => {
                    await updateDutyStatus(nextStatus, null, null);
                    setLocationStatus(nextStatus ? "Duty: Online (GPS unavailable)" : "Duty: Offline");
                },
                { enableHighAccuracy: true, timeout: 10000 }
            );
        } else {
            await updateDutyStatus(nextStatus, null, null);
            setLocationStatus(nextStatus ? "Duty: Online" : "Duty: Offline");
        }
    };

    const updateDutyStatus = async (online, latitude, longitude) => {
        try {
            const res = await fetch(`${API}/users/me/duty`, {
                method: "PUT",
                headers: authHeaders,
                body: JSON.stringify({ online, latitude, longitude }),
            });
            if (res.ok) {
                setIsOnline(online);
            }
        } catch (err) {
            console.error("Failed to update status", err);
        }
    };

    const captureDonorGps = () => {
        if (!navigator.geolocation) {
            setFoodGpsStatus("GPS is not supported by your browser.");
            return;
        }

        setFoodGpsStatus("Detecting current coordinates...");
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                setFoodLatitude(pos.coords.latitude);
                setFoodLongitude(pos.coords.longitude);
                setFoodGpsStatus(`Lat: ${pos.coords.latitude.toFixed(4)}, Lon: ${pos.coords.longitude.toFixed(4)}`);
            },
            () => {
                setFoodGpsStatus("Could not fetch location. Check browser location permissions.");
            },
            { enableHighAccuracy: true, timeout: 10000 }
        );
    };

    const fetchListings = async () => {
        try {
            const res = await fetch(`${API}/food-listings`, { headers: authHeaders });
            if (res.ok) {
                setListings(await res.json());
            }
        } catch (err) {
            console.error(err);
        }
    };

    const fetchVolunteerTasks = async () => {
        try {
            const res = await fetch(`${API}/tasks/volunteer/me`, { headers: authHeaders });
            if (res.ok) {
                setTasks(await res.json());
            }
        } catch (err) {
            console.error(err);
        }
    };

    const fetchNgoTasks = async () => {
        try {
            const res = await fetch(`${API}/tasks/ngo/me`, { headers: authHeaders });
            if (res.ok) {
                setTasks(await res.json());
            }
        } catch (err) {
            console.error(err);
        }
    };

    const fetchOnlineVolunteers = async () => {
        try {
            const res = await fetch(`${API}/users/volunteers/online`, { headers: authHeaders });
            if (res.ok) {
                setAvailableVolunteers(await res.json());
            }
        } catch (err) {
            console.error(err);
        }
    };

    const fetchPendingNgos = async () => {
        try {
            const res = await fetch(`${API}/admin/ngos/pending`, { headers: authHeaders });
            if (res.ok) {
                setPendingNgos(await res.json());
            }
        } catch (err) {
            console.error(err);
        }
    };

    const handleVerifyNgo = async (ngoId, approved) => {
        try {
            const res = await fetch(`${API}/admin/ngos/${ngoId}/verify`, {
                method: "POST",
                headers: authHeaders,
                body: JSON.stringify({ approved }),
            });
            if (res.ok) {
                setAdminMessage("NGO verification status updated.");
                fetchPendingNgos();
            }
        } catch (err) {
            console.error(err);
        }
    };

    const createListing = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch(`${API}/food-listings`, {
                method: "POST",
                headers: authHeaders,
                body: JSON.stringify({
                    title: foodTitle,
                    description: foodDescription,
                    quantity: Number(foodQuantity),
                    address: foodAddress,
                    latitude: foodLatitude,
                    longitude: foodLongitude,
                }),
            });

            if (res.ok) {
                setFoodTitle("");
                setFoodDescription("");
                setFoodQuantity("");
                setFoodAddress("");
                setFoodLatitude(null);
                setFoodLongitude(null);
                setFoodGpsStatus("");
                fetchListings();
            }
        } catch (err) {
            console.error(err);
        }
    };

    const assignVolunteer = async (listingId) => {
        const volId = selectedVolunteer[listingId];
        if (!volId) return;

        try {
            const res = await fetch(`${API}/tasks/assign`, {
                method: "POST",
                headers: authHeaders,
                body: JSON.stringify({ listingId, volunteerId: volId }),
            });
            if (res.ok) {
                fetchListings();
                fetchNgoTasks();
            }
        } catch (err) {
            console.error(err);
        }
    };

    const confirmDeliveryWithOtp = async (taskId) => {
        const code = pickupCodeInput[taskId];
        if (!code) return;

        try {
            const res = await fetch(`${API}/tasks/${taskId}/complete`, {
                method: "POST",
                headers: authHeaders,
                body: JSON.stringify({ pickupCode: code }),
            });
            if (res.ok) {
                fetchVolunteerTasks();
            }
        } catch (err) {
            console.error(err);
        }
    };

    const filteredListings = listings.filter((l) => {
        const matchesSearch =
            l.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            l.address.toLowerCase().includes(searchQuery.toLowerCase());
        if (filterTab === "ALL") return matchesSearch;
        return matchesSearch && l.status === filterTab;
    });

    if (!token) {
        return (
            <div className="app-container">
                {showVerification ? (
                    <div className="login-card verification-card">
                        <div className="verification-hero">
                            <span className="verification-icon">{"\u2709"}</span>
                        </div>
                        <h2>Verify your email</h2>
                        <p>We sent a 6-digit confirmation code to <strong className="verification-email">{verificationEmail}</strong></p>

                        {verificationMessage && <div className="msg">{verificationMessage}</div>}

                        <form className="verification-form" onSubmit={handleVerify}>
                            <div className="otp-grid">
                                {verificationCode.map((digit, index) => (
                                    <input
                                        key={index}
                                        id={`otp-${index}`}
                                        type="text"
                                        maxLength="1"
                                        className="otp-input"
                                        value={digit}
                                        onChange={(e) => {
                                            const val = e.target.value.slice(-1);
                                            const newCode = [...verificationCode];
                                            newCode[index] = val;
                                            setVerificationCode(newCode);
                                            if (val && index < 5) {
                                                document.getElementById(`otp-${index + 1}`)?.focus();
                                            }
                                        }}
                                        onKeyDown={(e) => {
                                            if (e.key === "Backspace" && !verificationCode[index] && index > 0) {
                                                document.getElementById(`otp-${index - 1}`)?.focus();
                                            }
                                        }}
                                    />
                                ))}
                            </div>

                            <button type="submit" className="submit-large primary" disabled={verificationLoading}>
                                {verificationLoading ? "Verifying..." : "Validate PIN"}
                            </button>
                        </form>
                    </div>
                ) : showResetPassword ? (
                    <div className="login-card">
                        <span className="eyebrow">SECURITY RECOVERY</span>
                        <h2>Set New Password</h2>
                        <p>Enter the reset token sent to your email and your new password.</p>

                        {resetMessage && <div className="msg">{resetMessage}</div>}

                        <form onSubmit={handleResetPassword}>
                            <label>Reset Token</label>
                            <input
                                type="text"
                                value={resetToken}
                                onChange={(e) => setResetToken(e.target.value)}
                                placeholder="Paste token here"
                                required
                            />

                            <label>New Password</label>
                            <input
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="Enter strong password"
                                required
                            />

                            <PasswordRequirements password={newPassword} />

                            <label>Confirm New Password</label>
                            <input
                                type="password"
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmNewPassword(e.target.value)}
                                placeholder="Confirm new password"
                                required
                            />

                            <button type="submit" className="submit-large primary" disabled={resetLoading}>
                                {resetLoading ? "Updating..." : "Update Password"}
                            </button>

                            <div className="auth-footer-nav">
                                <button type="button" className="text-link-button" onClick={() => setShowResetPassword(false)}>
                                    Back to Login
                                </button>
                            </div>
                        </form>
                    </div>
                ) : showSignup ? (
                    <div className="login-card">
                        <span className="eyebrow">JOIN SHAREPLATE</span>
                        <h2>Create your account</h2>
                        <p>Coordinate surplus food rescue across restaurants, shelters, and volunteers.</p>

                        {signupMessage && <div className="msg">{signupMessage}</div>}

                        <form onSubmit={handleRegister}>
                            <label>Full name</label>
                            <input
                                type="text"
                                value={signupName}
                                onChange={(e) => setSignupName(e.target.value)}
                                placeholder="e.g. Shakthi"
                                required
                            />

                            <label>Email</label>
                            <input
                                type="email"
                                value={signupEmail}
                                onChange={(e) => setSignupEmail(e.target.value)}
                                placeholder="name@organization.org"
                                required
                            />

                            <label>Phone number (for delivery coordination)</label>
                            <input
                                type="tel"
                                value={signupPhone}
                                onChange={(e) => setSignupPhone(e.target.value)}
                                placeholder="e.g. 9876543210"
                                required
                            />
                            <small className="field-hint">
                                Your phone number should look like: 10-digit mobile (e.g. 9876543210)
                            </small>

                            <label>Password</label>
                            <input
                                type="password"
                                value={signupPassword}
                                onChange={(e) => setSignupPassword(e.target.value)}
                                placeholder="Create a strong password"
                                required
                            />

                            <PasswordRequirements password={signupPassword} />

                            <label>Confirm password</label>
                            <input
                                type="password"
                                value={signupConfirmPassword}
                                onChange={(e) => setSignupConfirmPassword(e.target.value)}
                                placeholder="Re-enter your password"
                                required
                            />

                            <label>I want to join as</label>
                            <select value={signupRole} onChange={(e) => setSignupRole(e.target.value)}>
                                <option value="DONOR">Donor (Restaurant, Banquet, Individual)</option>
                                <option value="NGO">NGO (Shelter, Charity Organization)</option>
                                <option value="VOLUNTEER">Volunteer (Delivery Partner)</option>
                            </select>

                            <button type="submit" className="submit-large primary" disabled={signupLoading}>
                                {signupLoading ? "Creating Account..." : "Create Account"}
                            </button>

                            <div className="auth-footer-nav">
                                <span>Already have an account?</span>
                                <button type="button" className="text-link-button" onClick={() => setShowSignup(false)}>
                                    Sign in
                                </button>
                            </div>
                        </form>
                    </div>
                ) : (
                    <div className="login-card">
                        <span className="eyebrow">WELCOME BACK</span>
                        <h2>Sign in to SharePlate</h2>
                        <p>Rescue food, minimize waste, and serve communities in need.</p>

                        {loginMessage && <div className="msg">{loginMessage}</div>}

                        <form onSubmit={handleLogin}>
                            <label>Email</label>
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="name@organization.org"
                                required
                            />

                            <div className="password-header-row">
                                <label>Password</label>
                                <button
                                    type="button"
                                    className="inline-forgot-link"
                                    onClick={() => setShowForgotPassword(true)}
                                >
                                    Forgot password?
                                </button>
                            </div>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Your password"
                                required
                            />

                            <button type="submit" className="submit-large primary" disabled={loginLoading}>
                                {loginLoading ? "Authenticating..." : "Sign In"}
                            </button>

                            <div className="auth-footer-nav">
                                <span>Don't have an account?</span>
                                <button type="button" className="text-link-button" onClick={() => setShowSignup(true)}>
                                    Create account
                                </button>
                            </div>

                            <div className="auth-footer-nav" style={{ marginTop: "10px" }}>
                                <button type="button" className="text-button" onClick={() => setShowResetPassword(true)}>
                                    Have a reset token? Click here
                                </button>
                            </div>
                        </form>
                    </div>
                )}

                {showForgotPassword && (
                    <div className="modal-backdrop">
                        <div className="modal-card">
                            <div className="modal-header">
                                <h3>Reset Password</h3>
                                <button type="button" className="close-drawer" onClick={() => setShowForgotPassword(false)}>
                                    {"\u2715"}
                                </button>
                            </div>
                            <p className="history-intro">Enter your account email to receive a password reset token.</p>

                            {forgotMessage && <div className="msg">{forgotMessage}</div>}

                            <form onSubmit={handleForgotPassword}>
                                <label>Account Email</label>
                                <input
                                    type="email"
                                    value={forgotEmail}
                                    onChange={(e) => setForgotEmail(e.target.value)}
                                    placeholder="name@organization.org"
                                    required
                                />

                                <button type="submit" className="submit-large primary" disabled={forgotLoading}>
                                    {forgotLoading ? "Dispatching..." : "Send Reset Token"}
                                </button>
                            </form>
                        </div>
                    </div>
                )}
            </div>
        );
    }

    return (
        <div className="app-container">
            <header className="main-header">
                <div className="brand-group">
                    <span className="brand-icon">{"\u{1F372}"}</span>
                    <div>
                        <h1>SharePlate</h1>
                        <p>Real-Time Surplus Food Rescue & Rapid Logistics</p>
                    </div>
                </div>

                <div className="header-actions">
                    {user.role === "VOLUNTEER" && (
                        <button
                            type="button"
                            className={`duty-toggle-btn ${isOnline ? "online" : "offline"}`}
                            onClick={toggleVolunteerDuty}
                        >
                            <span className="duty-dot"></span>
                            {isOnline ? "Duty: Online" : "Duty: Offline"}
                        </button>
                    )}

                    <button type="button" className="theme-toggle" onClick={toggleTheme}>
                        {theme === "dark" ? "\u2600 Light" : "\u{1F319} Dark"}
                    </button>

                    <button type="button" className="hamburger-btn" onClick={() => setDrawerOpen(true)}>
                        <span className="hamburger-line"></span>
                        <span className="hamburger-line"></span>
                        <span className="hamburger-line"></span>
                    </button>
                </div>
            </header>

            {/* HAMBURGER SIDE DRAWER */}
            <div className={`drawer-backdrop ${drawerOpen ? "open" : ""}`} onClick={() => setDrawerOpen(false)} />
            <aside className={`menu-drawer ${drawerOpen ? "open" : ""}`}>
                <div className="drawer-header">
                    <h3>Control Panel</h3>
                    <button type="button" className="close-drawer" onClick={() => setDrawerOpen(false)}>
                        {"\u2715"}
                    </button>
                </div>

                <div className="drawer-user-info" onClick={() => { setShowProfileModal(true); setDrawerOpen(false); }}>
                    <div className="avatar-chip">
                        {userAvatar || user.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="user-text-col">
                        <strong>{user.name}</strong>
                        <small>{user.email}</small>
                        <span className="role-tag">{user.role}</span>
                    </div>
                </div>

                <nav className="drawer-nav">
                    <div className="nav-item active" onClick={() => setDrawerOpen(false)}>
                        <span>{"\u{1F3E0}"}</span> Overview
                    </div>
                    <div className="nav-item" onClick={() => { setShowHistoryModal(true); setDrawerOpen(false); }}>
                        <span>{"\u{1F4DC}"}</span> Rescue History & Activity
                    </div>
                    {user.role === "ADMIN" && (
                        <div className="nav-item highlight-nav" onClick={() => { setShowAdminModal(true); setDrawerOpen(false); }}>
                            <span>{"\u{1F6E1}\uFE0F"}</span> Admin Verification Portal
                        </div>
                    )}
                </nav>

                <button type="button" className="drawer-logout-btn" onClick={handleLogout}>
                    Sign Out
                </button>
            </aside>

            {/* USER PROFILE MODAL */}
            {showProfileModal && (
                <div className="modal-backdrop">
                    <div className="modal-card">
                        <div className="modal-header">
                            <h3>Account Profile</h3>
                            <button type="button" className="close-drawer" onClick={() => setShowProfileModal(false)}>
                                {"\u2715"}
                            </button>
                        </div>

                        <div className="avatar-preview-row">
                            <div className="avatar-chip large">
                                {userAvatar || user.name.charAt(0).toUpperCase()}
                            </div>
                            <div>
                                <h4>{user.name}</h4>
                                <p>{user.email}</p>
                                <span className="role-tag">{user.role}</span>
                            </div>
                        </div>

                        <label>Choose Avatar Icon</label>
                        <div className="avatar-preset-grid">
                            {["\u{1F468}\u200D\u{1F373}", "\u{1F372}", "\u{1F331}", "\u{1F6F5}", "\u{1F91D}", "\u{1F957}", "\u{1F4E6}", "\u{1F49A}", "\u26A1", "\u{1F31F}"].map((emoji, idx) => (
                                <button
                                    key={idx}
                                    type="button"
                                    className={`avatar-preset-btn ${userAvatar === emoji ? "selected" : ""}`}
                                    onClick={() => {
                                        setUserAvatar(emoji);
                                        localStorage.setItem("user_avatar", emoji);
                                    }}
                                >
                                    {emoji}
                                </button>
                            ))}
                        </div>

                        <button
                            type="button"
                            className="text-button remove-avatar-btn"
                            onClick={() => {
                                setUserAvatar("");
                                localStorage.removeItem("user_avatar");
                            }}
                        >
                            Reset to initials
                        </button>
                    </div>
                </div>
            )}

            {/* RESCUE HISTORY MODAL */}
            {showHistoryModal && (
                <div className="modal-backdrop">
                    <div className="modal-card">
                        <div className="modal-header">
                            <h3>Activity & Rescue Log</h3>
                            <button type="button" className="close-drawer" onClick={() => setShowHistoryModal(false)}>
                                {"\u2715"}
                            </button>
                        </div>
                        <p className="history-intro">Completed deliveries, pickups, and verified food rescues.</p>

                        <div className="history-list">
                            {tasks.filter(t => t.status === "DELIVERED" || t.status === "COMPLETED").map((t) => (
                                <div key={t.id} className="history-row">
                                    <div>
                                        <strong>{t.foodTitle}</strong>
                                        <div className="history-meta">
                                            <span>Task #{t.id}</span>
                                            <span>Donor: {t.donorPhone || "Shared"}</span>
                                        </div>
                                    </div>
                                    <div className="history-right">
                                        <span className="status-pill status-delivered">{"\u2713"} Delivered</span>
                                    </div>
                                </div>
                            ))}
                            {tasks.filter(t => t.status === "DELIVERED" || t.status === "COMPLETED").length === 0 && (
                                <div className="empty-history">No past rescues completed yet.</div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* ADMIN NGO VERIFICATION MODAL */}
            {showAdminModal && (
                <div className="modal-backdrop">
                    <div className="modal-card">
                        <div className="modal-header">
                            <h3>NGO Audit & Approval Portal</h3>
                            <button type="button" className="close-drawer" onClick={() => setShowAdminModal(false)}>
                                {"\u2715"}
                            </button>
                        </div>
                        <p className="history-intro">Review pending NGO registration credentials and assign operational clearance.</p>

                        {adminMessage && <div className="msg">{adminMessage}</div>}

                        <div className="history-list">
                            {pendingNgos.map((ngo) => (
                                <div key={ngo.id} className="history-row">
                                    <div>
                                        <strong>{ngo.name}</strong>
                                        <div className="history-meta">
                                            <span>{ngo.email}</span>
                                            <span>Phone: {ngo.phone || "N/A"}</span>
                                        </div>
                                    </div>
                                    <div style={{ display: "flex", gap: "8px" }}>
                                        <button
                                            type="button"
                                            className="gps-btn"
                                            onClick={() => handleVerifyNgo(ngo.id, true)}
                                        >
                                            Approve
                                        </button>
                                        <button
                                            type="button"
                                            className="text-button remove-avatar-btn"
                                            onClick={() => handleVerifyNgo(ngo.id, false)}
                                        >
                                            Reject
                                        </button>
                                    </div>
                                </div>
                            ))}
                            {pendingNgos.length === 0 && (
                                <div className="empty-history">No pending NGOs awaiting review.</div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* HERO BANNER */}
            <section className="modern-hero">
                <div>
                    <span className="eyebrow">{user.role} DASHBOARD</span>
                    <h2>Hello, {user.name} {"\u{1F44B}"}</h2>
                    <p>Coordinate surplus rescue, minimize city food waste, and support urgent relief efforts.</p>
                </div>
                <div className="metrics-strip">
                    <div className="metric-box highlight">
                        <span className="metric-value">{listings.length}</span>
                        <span className="metric-label">Listings</span>
                    </div>
                    <div className="metric-box">
                        <span className="metric-value">{tasks.length}</span>
                        <span className="metric-label">Active Tasks</span>
                    </div>
                </div>
            </section>

            {locationStatus && <div className="msg">{locationStatus}</div>}

            {/* ROLE: DONOR VIEW */}
            {user.role === "DONOR" && (
                <>
                    <section className="form-card">
                        <h3>Publish Surplus Food Donation</h3>
                        <form onSubmit={createListing}>
                            <div className="form-split">
                                <div>
                                    <label>Food Item / Title</label>
                                    <input
                                        type="text"
                                        value={foodTitle}
                                        onChange={(e) => setFoodTitle(e.target.value)}
                                        placeholder="e.g. 50 Packs of Cooked Meals"
                                        required
                                    />
                                </div>
                                <div>
                                    <label>Quantity (Portions / Servings)</label>
                                    <input
                                        type="number"
                                        value={foodQuantity}
                                        onChange={(e) => setFoodQuantity(e.target.value)}
                                        placeholder="50"
                                        required
                                    />
                                </div>
                            </div>

                            <label>Description & Expiry Details</label>
                            <textarea
                                value={foodDescription}
                                onChange={(e) => setFoodDescription(e.target.value)}
                                placeholder="Prepared at 1:00 PM, good until 8:00 PM. Hygienically packed."
                                rows="2"
                            />

                            <div className="label-with-action">
                                <label>Pickup Location Address</label>
                                <button type="button" className="gps-btn" onClick={captureDonorGps}>
                                    {"\u{1F4CD}"} Use Current GPS
                                </button>
                            </div>
                            <input
                                type="text"
                                value={foodAddress}
                                onChange={(e) => setFoodAddress(e.target.value)}
                                placeholder="Full street address, landmark, floor"
                                required
                            />
                            {foodGpsStatus && <small className="field-hint gps-active">{foodGpsStatus}</small>}

                            <button type="submit" className="submit-large primary">
                                Publish Surplus Food
                            </button>
                        </form>
                    </section>

                    <div className="controls-bar">
                        <div className="search-wrap">
                            <span className="search-icon">{"\u{1F50D}"}</span>
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder="Search by title or address..."
                            />
                            {searchQuery && (
                                <button type="button" className="clear-search" onClick={() => setSearchQuery("")}>
                                    {"\u2715"}
                                </button>
                            )}
                        </div>
                        <button type="button" className="refresh-pill" onClick={fetchListings}>
                            {"\u21BB"} Refresh
                        </button>
                    </div>

                    <div className="grid">
                        {filteredListings.map((l) => (
                            <article key={l.id} className="modern-card">
                                <div>
                                    <span className="listing-kicker">DONATION #{l.id}</span>
                                    <div className="top">
                                        <h3>{l.title}</h3>
                                        <span className={`status-pill status-${l.status.toLowerCase()}`}>{l.status}</span>
                                    </div>
                                    <p className="listing-description">{l.description}</p>
                                    <div className="meta-card">
                                        <div>{"\u{1F4E6}"} <strong>Quantity:</strong> {l.quantity} portions</div>
                                        <div>{"\u{1F4CD}"} <strong>Address:</strong> {l.address}</div>
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>
                </>
            )}

            {/* ROLE: NGO VIEW */}
            {user.role === "NGO" && (
                <>
                    <div className="controls-bar">
                        <div className="search-wrap">
                            <span className="search-icon">{"\u{1F50D}"}</span>
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder="Search available donations..."
                            />
                        </div>
                        <div className="filter-tabs">
                            {["ALL", "AVAILABLE", "CLAIMED"].map((tab) => (
                                <button
                                    key={tab}
                                    type="button"
                                    className={`tab-btn ${filterTab === tab ? "active" : ""}`}
                                    onClick={() => setFilterTab(tab)}
                                >
                                    {tab}
                                </button>
                            ))}
                        </div>
                    </div>

                    <h3>Available Surplus Donations</h3>
                    <div className="grid">
                        {filteredListings.filter(l => l.status === "AVAILABLE").map((l) => (
                            <article key={l.id} className="modern-card">
                                <div>
                                    <span className="listing-kicker">AVAILABLE SURPLUS</span>
                                    <div className="top">
                                        <h3>{l.title}</h3>
                                        <span className="status-pill status-available">AVAILABLE</span>
                                    </div>
                                    <p className="listing-description">{l.description}</p>
                                    <div className="meta-card">
                                        <div>{"\u{1F4E6}"} <strong>Quantity:</strong> {l.quantity} servings</div>
                                        <div>{"\u{1F4CD}"} <strong>Pickup:</strong> {l.address}</div>
                                        <div>{"\u{1F464}"} <strong>Donor:</strong> {l.donorName}</div>
                                    </div>

                                    {l.donorPhone && (
                                        <div className="delivery-contact-banner">
                                            <span>Donor Contact: <strong>{l.donorPhone}</strong></span>
                                            <a href={`tel:${l.donorPhone}`} className="call-btn">
                                                {"\u{1F4DE}"} Call Donor
                                            </a>
                                        </div>
                                    )}

                                    <div className="delivery-ops-panel">
                                        <label>Dispatch Volunteer:</label>
                                        <select
                                            value={selectedVolunteer[l.id] || ""}
                                            onChange={(e) => setSelectedVolunteer({ ...selectedVolunteer, [l.id]: e.target.value })}
                                        >
                                            <option value="">Choose On-Duty Volunteer</option>
                                            {availableVolunteers.map((v) => (
                                                <option key={v.id} value={v.id}>
                                                    {v.name} ({v.phone || "No phone"}) - {v.isOnline ? "\u{1F7E2} Online" : "Offline"}
                                                </option>
                                            ))}
                                        </select>
                                        <button
                                            type="button"
                                            className="submit-large primary"
                                            onClick={() => assignVolunteer(l.id)}
                                            disabled={!selectedVolunteer[l.id]}
                                        >
                                            Confirm & Dispatch Task
                                        </button>
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>

                    <h3 style={{ marginTop: "32px" }}>Active Rescues & Deliveries</h3>
                    <div className="grid">
                        {tasks.map((t) => (
                            <article key={t.id} className="task-card">
                                <div>
                                    <span className="listing-kicker">TASK #{t.id}</span>
                                    <div className="top">
                                        <h3>{t.foodTitle}</h3>
                                        <span className="status-pill status-assigned">{t.status}</span>
                                    </div>
                                    <div className="meta-card">
                                        <div>{"\u{1F6F5}"} <strong>Volunteer:</strong> {t.volunteerName} ({t.volunteerPhone || "No contact"})</div>
                                        <div>{"\u{1F4CD}"} <strong>Pickup Address:</strong> {t.pickupAddress}</div>
                                    </div>
                                    <div className="donor-code-pill">
                                        <span>Delivery Confirmation PIN:</span>
                                        <strong>{t.pickupCode}</strong>
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>
                </>
            )}

            {/* ROLE: VOLUNTEER VIEW */}
            {user.role === "VOLUNTEER" && (
                <>
                    <h3>Assigned Rescues</h3>
                    <div className="grid">
                        {tasks.map((t) => (
                            <article key={t.id} className="task-card">
                                <div>
                                    <span className="listing-kicker">PICKUP #{t.id}</span>
                                    <div className="top">
                                        <h3>{t.foodTitle}</h3>
                                        <span className={`status-pill status-${t.status.toLowerCase()}`}>{t.status}</span>
                                    </div>

                                    <div className="meta-card">
                                        <div>{"\u{1F4CD}"} <strong>Address:</strong> {t.pickupAddress}</div>
                                    </div>

                                    <div className="delivery-ops-panel">
                                        {t.donorPhone && (
                                            <div className="contact-row">
                                                <span>Donor: <strong>{t.donorPhone}</strong></span>
                                                <a href={`tel:${t.donorPhone}`} className="call-btn-small">
                                                    {"\u{1F4DE}"} Call Donor
                                                </a>
                                            </div>
                                        )}

                                        {t.latitude && t.longitude && (
                                            <a
                                                href={`https://www.google.com/maps/dir/?api=1&destination=${t.latitude},${t.longitude}`}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                className="nav-btn-gmaps"
                                            >
                                                {"\u{1F5FA}\uFE0F"} Open Driving Directions in Google Maps
                                            </a>
                                        )}
                                    </div>

                                    {t.status === "ASSIGNED" && (
                                        <div className="verification-box">
                                            <div>
                                                <strong>Confirm Delivery PIN</strong>
                                                <small>Ask the donor/recipient for the 4-digit PIN</small>
                                            </div>
                                            <div className="code-interactive-group">
                                                <input
                                                    type="text"
                                                    maxLength="4"
                                                    className="code-value"
                                                    value={pickupCodeInput[t.id] || ""}
                                                    onChange={(e) => setPickupCodeInput({ ...pickupCodeInput, [t.id]: e.target.value })}
                                                    placeholder="0000"
                                                />
                                                <button
                                                    type="button"
                                                    className="submit-large primary"
                                                    onClick={() => confirmDeliveryWithOtp(t.id)}
                                                >
                                                    Verify & Complete
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </article>
                        ))}
                    </div>
                </>
            )}

            {/* ROLE: ADMIN VIEW */}
            {user.role === "ADMIN" && (
                <>
                    <h3>Platform Operations & Audit</h3>
                    <section className="form-card">
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                            <div>
                                <h4>NGO Partner Clearances</h4>
                                <p style={{ color: "var(--sp-text-muted)", fontSize: "13px" }}>
                                    Manage charity and shelter access tokens to ensure legitimate food rescue operations.
                                </p>
                            </div>
                            <button
                                type="button"
                                className="submit-large primary"
                                style={{ width: "auto", margin: 0 }}
                                onClick={() => setShowAdminModal(true)}
                            >
                                Open NGO Review Queue ({pendingNgos.length})
                            </button>
                        </div>
                    </section>
                </>
            )}
        </div>
    );
}

ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);