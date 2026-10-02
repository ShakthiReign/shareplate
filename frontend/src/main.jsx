import React, { useEffect, useState, useMemo } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080/api").replace(/\/$/, "");

const authFetch = async (url, options = {}) => {
    const token = localStorage.getItem("shareplate_token");
    const headers = {
        ...(options.headers || {}),
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    return fetch(url, {
        ...options,
        headers,
    });
};

const checkPasswordRules = (pwd) => {
    const value = pwd || "";
    return {
        length: value.length >= 8 && value.length <= 72,
        upper: /[A-Z]/.test(value),
        lower: /[a-z]/.test(value),
        digit: /\d/.test(value),
        special: /[@$!%*?&#^()_+\-=[\]{}|;:,.<>/~]/.test(value),
    };
};

const isPasswordStrong = (pwd) => {
    const rules = checkPasswordRules(pwd);
    return rules.length && rules.upper && rules.lower && rules.digit && rules.special;
};

function PasswordRequirements({ password }) {
    const rules = checkPasswordRules(password);
    return (
        <div className="password-checklist">
            <span className="checklist-title">Password must contain:</span>
            <ul>
                <li className={rules.length ? "valid" : ""}>
                    <i>{rules.length ? "✓" : "○"}</i> 8 to 72 characters
                </li>
                <li className={rules.upper ? "valid" : ""}>
                    <i>{rules.upper ? "✓" : "○"}</i> At least 1 uppercase letter (A-Z)
                </li>
                <li className={rules.lower ? "valid" : ""}>
                    <i>{rules.lower ? "✓" : "○"}</i> At least 1 lowercase letter (a-z)
                </li>
                <li className={rules.digit ? "valid" : ""}>
                    <i>{rules.digit ? "✓" : "○"}</i> At least 1 digit (0-9)
                </li>
                <li className={rules.special ? "valid" : ""}>
                    <i>{rules.special ? "✓" : "○"}</i> At least 1 special character (!@#$%...)
                </li>
            </ul>
        </div>
    );
}

function App() {
    const [user, setUser] = useState(null);

    // HAMBURGER / MODAL STATES
    const [menuOpen, setMenuOpen] = useState(false);
    const [activeModal, setActiveModal] = useState(null); // 'profile' | 'history' | 'security' | 'ngo-verify' | null

    // NGO VERIFICATION FORM
    const [darpanId, setDarpanId] = useState("");
    const [ngoPhone, setNgoPhone] = useState("");
    const [ngoVerifyLoading, setNgoVerifyLoading] = useState(false);
    const [ngoVerifyMsg, setNgoVerifyMsg] = useState("");
    const [pendingClaimListingId, setPendingClaimListingId] = useState(null);

    // PROFILE MANAGEMENT
    const [customAvatar, setCustomAvatar] = useState(() => localStorage.getItem("shareplate_avatar") || "");
    const [oldPassword, setOldPassword] = useState("");
    const [newPasswordVal, setNewPasswordVal] = useState("");
    const [confirmNewPasswordVal, setConfirmNewPasswordVal] = useState("");
    const [changePasswordMsg, setChangePasswordMsg] = useState("");

    // LOGIN
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loginMessage, setLoginMessage] = useState("");
    const [loading, setLoading] = useState(false);

    // THEME
    const [theme, setTheme] = useState(() => {
        const saved = localStorage.getItem("shareplate_theme");
        if (saved === "light" || saved === "dark") return saved;
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    });

    // SIGNUP
    const [showSignup, setShowSignup] = useState(false);
    const [signupName, setSignupName] = useState("");
    const [signupEmail, setSignupEmail] = useState("");
    const [signupPassword, setSignupPassword] = useState("");
    const [signupConfirmPassword, setSignupConfirmPassword] = useState("");
    const [signupRole, setSignupRole] = useState("DONOR");
    const [signupMessage, setSignupMessage] = useState("");
    const [signupLoading, setSignupLoading] = useState(false);

    // EMAIL VERIFICATION
    const [showVerification, setShowVerification] = useState(false);
    const [verificationEmail, setVerificationEmail] = useState("");
    const [verificationCode, setVerificationCode] = useState(["", "", "", "", "", ""]);
    const [verificationMessage, setVerificationMessage] = useState("");
    const [verificationLoading, setVerificationLoading] = useState(false);
    const [resendLoading, setResendLoading] = useState(false);
    const [resendCooldown, setResendCooldown] = useState(0);

    // FORGOT / RESET PASSWORD
    const [showForgotPassword, setShowForgotPassword] = useState(false);
    const [forgotPasswordEmail, setForgotPasswordEmail] = useState("");
    const [forgotPasswordMessage, setForgotPasswordMessage] = useState("");
    const [forgotPasswordLoading, setForgotPasswordLoading] = useState(false);
    const [showResetPassword, setShowResetPassword] = useState(false);
    const [resetToken, setResetToken] = useState("");
    const [resetPassword, setResetPassword] = useState("");
    const [resetConfirmPassword, setResetConfirmPassword] = useState("");
    const [resetPasswordMessage, setResetPasswordMessage] = useState("");
    const [resetPasswordLoading, setResetPasswordLoading] = useState(false);
    const [resetTokenChecking, setResetTokenChecking] = useState(false);

    // FOOD LISTINGS
    const [listings, setListings] = useState([]);
    const [msg, setMsg] = useState("");

    const [foodName, setFoodName] = useState("");
    const [description, setDescription] = useState("");
    const [quantity, setQuantity] = useState("");
    const [location, setLocation] = useState("");
    const [pickupDeadline, setPickupDeadline] = useState("");
    const [safetyDetails, setSafetyDetails] = useState("");
    const [posting, setPosting] = useState(false);

    // FILTER & SEARCH
    const [searchTerm, setSearchTerm] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [copiedCodeKey, setCopiedCodeKey] = useState(null);

    // VOLUNTEER
    const [tasks, setTasks] = useState([]);
    const [taskMessage, setTaskMessage] = useState("");
    const [handoverCodes, setHandoverCodes] = useState({});

    // DONOR / NGO VERIFICATION CODES
    const [donorTasks, setDonorTasks] = useState([]);
    const [pickupCodes, setPickupCodes] = useState({});
    const [deliveryCodes, setDeliveryCodes] = useState({});
    const [codeLoading, setCodeLoading] = useState({});

    // NGO
    const [volunteers, setVolunteers] = useState([]);
    const [ngoTasks, setNgoTasks] = useState([]);
    const [selectedVolunteers, setSelectedVolunteers] = useState({});
    const [assigningListing, setAssigningListing] = useState(null);

    // GLOBAL THEME APPLICATION
    useEffect(() => {
        document.documentElement.setAttribute("data-theme", theme);
        document.body.setAttribute("data-theme", theme);
        localStorage.setItem("shareplate_theme", theme);
    }, [theme]);

    const toggleTheme = () => {
        setTheme((current) => (current === "dark" ? "light" : "dark"));
    };

    useEffect(() => {
        if (resendCooldown <= 0) return;
        const timer = window.setInterval(() => {
            setResendCooldown((current) => Math.max(0, current - 1));
        }, 1000);
        return () => window.clearInterval(timer);
    }, [resendCooldown]);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const token = params.get("token");

        if (window.location.pathname === "/reset-password" && token) {
            setResetToken(token);
            setShowResetPassword(true);
            setShowSignup(false);
            setShowVerification(false);
            setShowForgotPassword(false);
            setResetTokenChecking(true);
            setResetPasswordMessage("");

            fetch(`${API}/users/validate-reset-token?token=${encodeURIComponent(token)}`)
                .then(async (response) => {
                    if (!response.ok) {
                        let message = "This password reset link is invalid or expired.";
                        try {
                            const data = await response.json();
                            message = data.error || data.message || message;
                        } catch {}
                        throw new Error(message);
                    }
                })
                .catch((error) => {
                    setResetPasswordMessage(error.message || "This password reset link is invalid or expired.");
                })
                .finally(() => setResetTokenChecking(false));
        }
    }, []);

    const clearResetRoute = () => {
        if (window.location.pathname === "/reset-password") {
            window.history.replaceState({}, document.title, window.location.origin + window.location.pathname);
        }
    };

    const copyCode = (code, key) => {
        if (!code || code === "Loading..." || code === "Unavailable") return;
        navigator.clipboard.writeText(code).then(() => {
            setCopiedCodeKey(key);
            setTimeout(() => setCopiedCodeKey(null), 2000);
        });
    };

    const handleAvatarSelect = (emoji) => {
        setCustomAvatar(emoji);
        localStorage.setItem("shareplate_avatar", emoji);
    };

    const handleAvatarUpload = (e) => {
        const file = e.target.files?.[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = () => {
                setCustomAvatar(reader.result);
                localStorage.setItem("shareplate_avatar", reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleRemoveAvatar = () => {
        setCustomAvatar("");
        localStorage.removeItem("shareplate_avatar");
    };

    const handleChangePasswordSubmit = (e) => {
        e.preventDefault();
        setChangePasswordMsg("");

        if (newPasswordVal !== confirmNewPasswordVal) {
            setChangePasswordMsg("New passwords do not match.");
            return;
        }

        if (!isPasswordStrong(newPasswordVal)) {
            setChangePasswordMsg("Password does not meet the security criteria.");
            return;
        }

        setChangePasswordMsg("Password updated successfully.");
        setOldPassword("");
        setNewPasswordVal("");
        setConfirmNewPasswordVal("");
    };

    const submitNgoVerification = async (e) => {
        e.preventDefault();
        setNgoVerifyLoading(true);
        setNgoVerifyMsg("");

        try {
            const response = await authFetch(`${API}/users/${user.id}/verify-ngo`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ darpanId, phone: ngoPhone }),
            });

            if (!response.ok) {
                let err = "Verification failed. Check your Darpan ID & Phone.";
                try {
                    const data = await response.json();
                    err = data.message || data.error || err;
                } catch {}
                throw new Error(err);
            }

            // Successfully verified! Update user in local state
            setUser((prev) => ({ ...prev, ngoVerified: true }));
            setActiveModal(null);
            setNgoVerifyMsg("");

            // If user clicked claim on a listing, auto-trigger claim
            if (pendingClaimListingId) {
                await executeClaim(pendingClaimListingId);
                setPendingClaimListingId(null);
            } else {
                setMsg("NGO verified successfully! You can now claim surplus food listings.");
            }
        } catch (error) {
            setNgoVerifyMsg(error.message || "Could not complete verification.");
        } finally {
            setNgoVerifyLoading(false);
        }
    };

    const openForgotPassword = () => {
        setShowSignup(false);
        setShowVerification(false);
        setShowForgotPassword(true);
        setShowResetPassword(false);
        setForgotPasswordEmail(email.trim());
        setForgotPasswordMessage("");
        setLoginMessage("");
    };

    const openLogin = () => {
        setShowSignup(false);
        setShowVerification(false);
        setShowForgotPassword(false);
        setShowResetPassword(false);
        setSignupMessage("");
        setForgotPasswordMessage("");
        setResetPasswordMessage("");
        setLoginMessage("");
        clearResetRoute();
    };

    const requestPasswordReset = async (event) => {
        event.preventDefault();
        setForgotPasswordLoading(true);
        setForgotPasswordMessage("");

        try {
            const response = await fetch(`${API}/users/forgot-password`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: forgotPasswordEmail.trim() }),
            });

            let message = "If an account exists for this email, a password reset link has been sent.";
            try {
                const data = await response.json();
                message = data.message || data.error || message;
            } catch {}

            if (!response.ok) throw new Error(message);
            setForgotPasswordMessage(message);
        } catch (error) {
            setForgotPasswordMessage(error.message || "Could not process the password reset request.");
        } finally {
            setForgotPasswordLoading(false);
        }
    };

    const resetPasswordSubmit = async (event) => {
        event.preventDefault();

        if (resetPassword !== resetConfirmPassword) {
            setResetPasswordMessage("Passwords do not match.");
            return;
        }

        if (!isPasswordStrong(resetPassword)) {
            setResetPasswordMessage("Please fulfill all password requirements below.");
            return;
        }

        setResetPasswordLoading(true);
        setResetPasswordMessage("");

        try {
            const response = await fetch(`${API}/users/reset-password`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    token: resetToken,
                    newPassword: resetPassword,
                    confirmPassword: resetConfirmPassword,
                }),
            });

            let message = "Password reset successfully. You can now log in.";
            try {
                const data = await response.json();
                message = data.message || data.error || message;
            } catch {}

            if (!response.ok) throw new Error(message);

            setResetPassword("");
            setResetConfirmPassword("");
            setEmail("");
            setPassword("");
            setResetPasswordMessage("");
            clearResetRoute();
            setShowResetPassword(false);
            setLoginMessage("Password reset successfully. You can now sign in.");
        } catch (error) {
            setResetPasswordMessage(error.message || "Could not reset your password.");
        } finally {
            setResetPasswordLoading(false);
        }
    };

    const loadListings = async () => {
        try {
            const response = await authFetch(`${API}/listings`);
            if (!response.ok) throw new Error();
            const data = await response.json();
            setListings(data);
        } catch {
            setMsg("Start the Spring Boot backend first.");
        }
    };

    const loadTasks = async (volunteerId) => {
        try {
            const response = await authFetch(`${API}/tasks/volunteer/${volunteerId}`);
            if (!response.ok) throw new Error();
            const data = await response.json();
            setTasks(data);
        } catch {
            setTaskMessage("Could not load your pickup tasks.");
        }
    };

    const loadAllTasksForDonor = async () => {
        try {
            const response = await authFetch(`${API}/tasks`);
            if (!response.ok) throw new Error();
            const data = await response.json();
            setDonorTasks(data);
        } catch {
            setMsg("Could not load your pickup assignments.");
        }
    };

    const loadVolunteers = async () => {
        try {
            const response = await authFetch(`${API}/users/volunteers`);
            if (!response.ok) throw new Error();
            const data = await response.json();
            setVolunteers(data);
        } catch {
            setMsg("Could not load volunteers.");
        }
    };

    const loadNgoTasks = async () => {
        try {
            const response = await authFetch(`${API}/tasks`);
            if (!response.ok) throw new Error();
            const data = await response.json();
            setNgoTasks(data);
        } catch {
            setMsg("Could not load pickup assignments.");
        }
    };

    useEffect(() => {
        if (user) loadListings();
    }, [user]);

    useEffect(() => {
        if (!user) {
            setTasks([]);
            setDonorTasks([]);
            setVolunteers([]);
            setNgoTasks([]);
            return;
        }

        if (user.role === "VOLUNTEER") loadTasks(user.id);
        if (user.role === "DONOR") loadAllTasksForDonor();
        if (user.role === "NGO") {
            loadVolunteers();
            loadNgoTasks();
        }
    }, [user]);

    // DONOR: load pickup codes
    useEffect(() => {
        if (user?.role !== "DONOR") {
            setPickupCodes({});
            return;
        }

        const ownListingIds = new Set(
            listings
                .filter((listing) => Number(listing.donorId) === Number(user.id))
                .map((listing) => Number(listing.id))
        );

        const relevantTasks = donorTasks.filter((task) =>
            ownListingIds.has(Number(task.listingId))
        );

        const fetchCodes = async () => {
            for (const task of relevantTasks) {
                if (pickupCodes[task.id]) continue;

                try {
                    setCodeLoading((previous) => ({
                        ...previous,
                        [`pickup-${task.id}`]: true,
                    }));

                    const response = await authFetch(`${API}/tasks/${task.id}/pickup-code`);
                    if (!response.ok) continue;

                    const data = await response.json();
                    setPickupCodes((previous) => ({
                        ...previous,
                        [task.id]: data.code,
                    }));
                } catch {
                } finally {
                    setCodeLoading((previous) => ({
                        ...previous,
                        [`pickup-${task.id}`]: false,
                    }));
                }
            }
        };

        if (relevantTasks.length > 0) fetchCodes();
    }, [user, listings, donorTasks]);

    // NGO: load delivery codes
    useEffect(() => {
        if (user?.role !== "NGO") {
            setDeliveryCodes({});
            return;
        }

        const relevantTasks = ngoTasks.filter((task) => {
            const listing = listings.find((item) => Number(item.id) === Number(task.listingId));
            return listing && Number(listing.claimedByNgoId) === Number(user.id);
        });

        const fetchCodes = async () => {
            for (const task of relevantTasks) {
                if (deliveryCodes[task.id]) continue;

                try {
                    setCodeLoading((previous) => ({
                        ...previous,
                        [`delivery-${task.id}`]: true,
                    }));

                    const response = await authFetch(`${API}/tasks/${task.id}/delivery-code`);
                    if (!response.ok) continue;

                    const data = await response.json();
                    setDeliveryCodes((previous) => ({
                        ...previous,
                        [task.id]: data.code,
                    }));
                } catch {
                } finally {
                    setCodeLoading((previous) => ({
                        ...previous,
                        [`delivery-${task.id}`]: false,
                    }));
                }
            }
        };

        if (relevantTasks.length > 0) fetchCodes();
    }, [user, listings, ngoTasks]);

    // LOGIN
    const login = async (event) => {
        event.preventDefault();
        setLoading(true);
        setLoginMessage("");

        try {
            const response = await fetch(`${API}/users/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });

            if (!response.ok) {
                let errorMessage = "Invalid email or password.";
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.error || errorData.message || errorMessage;
                } catch {}
                throw new Error(errorMessage);
            }

            const authResponse = await response.json();
            localStorage.setItem("shareplate_token", authResponse.token);
            setUser(authResponse.user);
            setPassword("");
        } catch (error) {
            const message = error?.message || "";
            if (message.toLowerCase().includes("verify your email")) {
                setVerificationEmail(email.trim());
                setVerificationCode(["", "", "", "", "", ""]);
                setVerificationMessage("Please verify your email before signing in.");
                setShowVerification(true);
            } else {
                setLoginMessage("Invalid email or password.");
            }
        } finally {
            setLoading(false);
        }
    };

    // SIGNUP
    const register = async (event) => {
        event.preventDefault();
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
            const response = await fetch(`${API}/users/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: signupName.trim(),
                    email: signupEmail.trim(),
                    password: signupPassword,
                    role: signupRole,
                }),
            });

            if (!response.ok) {
                let errorMessage = "Could not create account.";
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorData.error || errorMessage;
                } catch {
                    const errorText = await response.text();
                    if (errorText) errorMessage = errorText;
                }
                throw new Error(errorMessage);
            }

            await response.json();

            const registeredEmail = signupEmail.trim().toLowerCase();
            setVerificationEmail(registeredEmail);
            setVerificationCode(["", "", "", "", "", ""]);
            setVerificationMessage("We sent a 6-digit verification code to your email.");
            setShowVerification(true);
            setShowSignup(false);

            setEmail(registeredEmail);
            setPassword("");
            setSignupName("");
            setSignupEmail("");
            setSignupPassword("");
            setSignupConfirmPassword("");
            setSignupRole("DONOR");
            setSignupMessage("");
            setLoginMessage("");
        } catch (error) {
            setSignupMessage(error.message || "Could not create account.");
        } finally {
            setSignupLoading(false);
        }
    };

    const updateVerificationDigit = (index, value) => {
        const digits = String(value || "").replace(/\D/g, "").slice(0, 6);
        const next = [...verificationCode];

        if (digits.length > 1) {
            digits.split("").forEach((digit, offset) => {
                if (index + offset < 6) next[index + offset] = digit;
            });
            setVerificationCode(next);
            document.getElementById(`verification-${Math.min(index + digits.length, 5)}`)?.focus();
            return;
        }

        next[index] = digits;
        setVerificationCode(next);
        if (digits && index < 5) document.getElementById(`verification-${index + 1}`)?.focus();
    };

    const handleVerificationKeyDown = (event, index) => {
        if (event.key === "Backspace" && !verificationCode[index] && index > 0) {
            document.getElementById(`verification-${index - 1}`)?.focus();
        }
    };

    const verifyEmail = async (event) => {
        event.preventDefault();
        const code = verificationCode.join("");
        if (code.length !== 6) {
            setVerificationMessage("Enter all 6 digits from the email.");
            return;
        }
        setVerificationLoading(true);
        setVerificationMessage("");
        try {
            const response = await fetch(`${API}/users/verify-email`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: verificationEmail.trim(), code }),
            });
            if (!response.ok) {
                let errorMessage = "Could not verify your email.";
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.error || errorData.message || errorMessage;
                } catch {}
                throw new Error(errorMessage);
            }
            setShowVerification(false);
            setVerificationCode(["", "", "", "", "", ""]);
            setVerificationMessage("");
            setEmail(verificationEmail.trim());
            setPassword("");
            setLoginMessage("Email verified successfully. You can now sign in.");
        } catch (error) {
            setVerificationMessage(error.message || "Could not verify your email.");
        } finally {
            setVerificationLoading(false);
        }
    };

    const resendVerification = async () => {
        if (resendCooldown > 0 || resendLoading) return;
        setResendLoading(true);
        setVerificationMessage("");
        try {
            const response = await fetch(`${API}/users/resend-verification`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: verificationEmail.trim() }),
            });
            if (!response.ok) {
                let errorMessage = "Could not resend the verification code.";
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.error || errorData.message || errorMessage;
                } catch {}
                throw new Error(errorMessage);
            }
            setVerificationCode(["", "", "", "", "", ""]);
            setVerificationMessage("A new verification code has been sent.");
            setResendCooldown(30);
        } catch (error) {
            setVerificationMessage(error.message || "Could not resend the verification code.");
        } finally {
            setResendLoading(false);
        }
    };

    const openSignup = () => {
        setShowSignup(true);
        setShowVerification(false);
        setShowForgotPassword(false);
        setShowResetPassword(false);
        setSignupMessage("");
        setLoginMessage("");
    };

    const logout = () => {
        localStorage.removeItem("shareplate_token");
        setUser(null);
        setMenuOpen(false);
        setActiveModal(null);
        setEmail("");
        setPassword("");
        setLoginMessage("");
        setMsg("");
        setTaskMessage("");
        setTasks([]);
        setDonorTasks([]);
        setVolunteers([]);
        setNgoTasks([]);
        setSelectedVolunteers({});
        setHandoverCodes({});
        setPickupCodes({});
        setDeliveryCodes({});
    };

    // DONOR - POST FOOD
    const getDeadlineBounds = () => {
        const now = new Date();
        const minimum = new Date(now.getTime() + 30 * 60 * 1000);
        const maximum = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
        const toLocalInput = (date) => {
            const offset = date.getTimezoneOffset();
            return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16);
        };
        return { min: toLocalInput(minimum), max: toLocalInput(maximum) };
    };

    const validatePickupDeadline = (value) => {
        if (!value) return "Please choose a pickup deadline.";
        const deadline = new Date(value);
        if (Number.isNaN(deadline.getTime())) return "Please choose a valid pickup date and time.";

        const now = Date.now();
        const minimum = now + 30 * 60 * 1000;
        const maximum = now + 7 * 24 * 60 * 60 * 1000;

        if (deadline.getTime() < now) return "Pickup deadline cannot be in the past.";
        if (deadline.getTime() < minimum) return "Pickup deadline must be at least 30 minutes from now.";
        if (deadline.getTime() > maximum) return "Pickup deadline cannot be more than 7 days from now.";
        return "";
    };

    const postFood = async (event) => {
        event.preventDefault();
        setPosting(true);
        setMsg("");

        try {
            const response = await authFetch(`${API}/listings`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    foodName,
                    description,
                    quantity: Number(quantity),
                    location,
                    pickupDeadline,
                    safetyDetails,
                    donorId: user.id,
                }),
            });

            if (!response.ok) throw new Error();

            setFoodName("");
            setDescription("");
            setQuantity("");
            setLocation("");
            setPickupDeadline("");
            setSafetyDetails("");
            setMsg("Food listing posted successfully.");
            await loadListings();
        } catch {
            setMsg("Could not post food listing.");
        } finally {
            setPosting(false);
        }
    };

    // NGO - CLAIM (WITH MODAL INTERCEPT IF UNVERIFIED)
    const handleClaimClick = (listingId) => {
        if (!user || user.role !== "NGO") {
            setMsg("Only an NGO user can claim a listing.");
            return;
        }

        // If NGO is not verified yet, open the verification modal smoothly
        if (!user.ngoVerified) {
            setPendingClaimListingId(listingId);
            setActiveModal("ngo-verify");
            return;
        }

        executeClaim(listingId);
    };

    const executeClaim = async (listingId) => {
        try {
            const response = await authFetch(`${API}/listings/${listingId}/claim/${user.id}`, {
                method: "POST",
            });

            if (!response.ok) {
                let errorText = "Could not claim listing.";
                try {
                    const data = await response.json();
                    errorText = data.message || data.error || errorText;
                } catch {
                    const txt = await response.text();
                    if (txt) errorText = txt;
                }
                throw new Error(errorText);
            }

            setMsg("Listing claimed successfully.");
            await loadListings();
        } catch (error) {
            setMsg(error.message || "Could not claim listing.");
        }
    };

    const selectVolunteer = (listingId, volunteerId) => {
        setSelectedVolunteers((previous) => ({
            ...previous,
            [listingId]: volunteerId,
        }));
    };

    const getTaskForListing = (listingId, source = ngoTasks) => {
        return source.find((task) => Number(task.listingId) === Number(listingId));
    };

    const getVolunteer = (volunteerId) => {
        return volunteers.find((volunteer) => Number(volunteer.id) === Number(volunteerId));
    };

    // NGO - ASSIGN VOLUNTEER
    const assignVolunteer = async (listingId) => {
        const volunteerId = selectedVolunteers[listingId];

        if (!volunteerId) {
            setMsg("Please select a volunteer first.");
            return;
        }

        const existingTask = getTaskForListing(listingId);
        if (existingTask) {
            setMsg("A volunteer is already assigned to this listing.");
            return;
        }

        setAssigningListing(listingId);
        setMsg("");

        try {
            const response = await authFetch(
                `${API}/tasks?listingId=${listingId}&volunteerId=${volunteerId}`,
                { method: "POST" }
            );

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Assignment failed");
            }

            await loadNgoTasks();
            await loadAllTasksForDonor();

            setSelectedVolunteers((previous) => ({
                ...previous,
                [listingId]: "",
            }));

            setMsg("Volunteer assigned successfully.");
        } catch (error) {
            setMsg(error.message || "Could not assign volunteer.");
        } finally {
            setAssigningListing(null);
        }
    };

    const updateHandoverCode = (taskId, code) => {
        const numericCode = code.replace(/\D/g, "").slice(0, 6);
        setHandoverCodes((previous) => ({
            ...previous,
            [taskId]: numericCode,
        }));
    };

    // VOLUNTEER - COLLECT
    const collectTask = async (taskId) => {
        const code = handoverCodes[taskId];

        if (!code || code.trim() === "") {
            setTaskMessage("Enter the pickup code first.");
            return;
        }

        try {
            const response = await authFetch(`${API}/tasks/${taskId}/collect`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ code: code.trim() }),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            setTaskMessage("Food collected successfully.");
            setHandoverCodes((previous) => ({
                ...previous,
                [taskId]: "",
            }));
            await loadTasks(user.id);
        } catch (error) {
            setTaskMessage(error.message || "Could not collect food. Check the pickup code.");
        }
    };

    // VOLUNTEER - DELIVER
    const deliverTask = async (taskId) => {
        const code = handoverCodes[taskId];

        if (!code || code.trim() === "") {
            setTaskMessage("Enter the delivery code first.");
            return;
        }

        try {
            const response = await authFetch(`${API}/tasks/${taskId}/deliver`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ code: code.trim() }),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            setTaskMessage("Food delivered successfully.");
            setHandoverCodes((previous) => ({
                ...previous,
                [taskId]: "",
            }));
            await loadTasks(user.id);
        } catch (error) {
            setTaskMessage(error.message || "Could not mark the food as delivered.");
        }
    };

    const refreshAll = async () => {
        await loadListings();

        if (user.role === "NGO") {
            await loadNgoTasks();
            await loadVolunteers();
        }

        if (user.role === "DONOR") {
            await loadAllTasksForDonor();
        }

        if (user.role === "VOLUNTEER") {
            await loadTasks(user.id);
        }
    };

    const donorOwnTasks = donorTasks.filter((task) => {
        const listing = listings.find((item) => Number(item.id) === Number(task.listingId));
        return listing && Number(listing.donorId) === Number(user?.id);
    });

    // COMPUTED ANALYTICS / METRICS (FIXED BASE-10 INTEGER ADDITION - NO MORE 909)
    const stats = useMemo(() => {
        let totalMeals = 0;
        for (const item of listings) {
            const rawVal = item.quantity ?? item.meals ?? 0;
            const num = Number(rawVal);
            if (!Number.isNaN(num)) {
                totalMeals += num;
            }
        }

        const deliveredCount = listings.filter((l) => {
            const task = donorTasks.find((t) => Number(t.listingId) === Number(l.id)) ||
                         ngoTasks.find((t) => Number(t.listingId) === Number(l.id)) ||
                         tasks.find((t) => Number(t.listingId) === Number(l.id));
            return task?.status === "DELIVERED";
        }).length;

        const activeRescues = listings.filter((l) => l.status === "CLAIMED").length;
        return { totalMeals, deliveredCount, activeRescues };
    }, [listings, donorTasks, ngoTasks, tasks]);

    // FILTERED LISTINGS
    const filteredListings = useMemo(() => {
        return listings.filter((listing) => {
            const matchesSearch =
                (listing.foodName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
                (listing.location || "").toLowerCase().includes(searchTerm.toLowerCase());

            if (!matchesSearch) return false;

            const listingTask =
                donorTasks.find((t) => Number(t.listingId) === Number(listing.id)) ||
                ngoTasks.find((t) => Number(t.listingId) === Number(listing.id)) ||
                tasks.find((t) => Number(t.listingId) === Number(listing.id));
            const isCompleted = listingTask?.status === "DELIVERED";

            if (statusFilter === "AVAILABLE") return listing.status === "AVAILABLE";
            if (statusFilter === "CLAIMED") return listing.status === "CLAIMED" && !isCompleted;
            if (statusFilter === "DELIVERED") return isCompleted;
            return true;
        });
    }, [listings, searchTerm, statusFilter, donorTasks, ngoTasks, tasks]);

    // ROLE-SPECIFIC RESCUE HISTORY LOG
    const userHistory = useMemo(() => {
        if (!user) return [];
        if (user.role === "DONOR") {
            return listings
                .filter((l) => Number(l.donorId) === Number(user.id))
                .map((l) => {
                    const task = donorTasks.find((t) => Number(t.listingId) === Number(l.id));
                    return {
                        id: l.id,
                        title: l.foodName,
                        quantity: l.quantity,
                        location: l.location,
                        status: task?.status === "DELIVERED" ? "DELIVERED" : l.status,
                        time: l.pickupDeadline,
                    };
                });
        }
        if (user.role === "NGO") {
            return listings
                .filter((l) => Number(l.claimedByNgoId) === Number(user.id))
                .map((l) => {
                    const task = ngoTasks.find((t) => Number(t.listingId) === Number(l.id));
                    return {
                        id: l.id,
                        title: l.foodName,
                        quantity: l.quantity,
                        location: l.location,
                        status: task?.status === "DELIVERED" ? "DELIVERED" : "COORDINATING",
                        time: l.pickupDeadline,
                    };
                });
        }
        if (user.role === "VOLUNTEER") {
            return tasks.map((t) => {
                const listing = listings.find((l) => Number(l.id) === Number(t.listingId));
                return {
                    id: t.id,
                    title: listing?.foodName || `Pickup Task #${t.id}`,
                    quantity: listing?.quantity || "–",
                    location: listing?.location || "–",
                    status: t.status,
                    time: t.deliveredAt || t.collectedAt || "Pending",
                };
            });
        }
        return [];
    }, [user, listings, donorTasks, ngoTasks, tasks]);

    const getUrgencyBadge = (deadlineStr) => {
        if (!deadlineStr) return null;
        const diffMs = new Date(deadlineStr).getTime() - Date.now();
        const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
        if (diffMs <= 0) return <span className="urgency-badge expired">⚠️️ Expired</span>;
        if (diffHrs < 3) return <span className="urgency-badge critical">🔥 &lt;3h left</span>;
        if (diffHrs < 12) return <span className="urgency-badge urgent">⏱ {diffHrs}h left</span>;
        return <span className="urgency-badge calm">⏱ {diffHrs}h left</span>;
    };

    const renderAvatarDisplay = (size = "normal") => {
        if (customAvatar && customAvatar.startsWith("data:image")) {
            return <img src={customAvatar} alt="Profile" className={`avatar-img ${size}`} />;
        }
        if (customAvatar) {
            return <div className={`avatar-chip ${size}`}>{customAvatar}</div>;
        }
        return <div className={`avatar-chip ${size}`}>{user ? user.name.charAt(0).toUpperCase() : "U"}</div>;
    };

    return (
        <div className="app-container">
            {/* TOP NAVIGATION BAR */}
            <header className="main-header">
                <div className="brand-group">
                    <div className="brand-icon">🍲</div>
                    <div>
                        <h1>SharePlate</h1>
                        <p>Surplus food rescue & coordination</p>
                    </div>
                </div>

                <div className="header-actions">
                    <button
                        type="button"
                        className="theme-toggle"
                        onClick={toggleTheme}
                        aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} theme`}
                    >
                        {theme === "dark" ? "☀ Light" : "☾ Dark"}
                    </button>

                    {user && (
                        <button
                            type="button"
                            className="hamburger-btn"
                            onClick={() => setMenuOpen(!menuOpen)}
                            aria-label="Toggle Navigation Menu"
                        >
                            <span className="hamburger-line"></span>
                            <span className="hamburger-line"></span>
                            <span className="hamburger-line"></span>
                        </button>
                    )}
                </div>
            </header>

            {/* SLIDE-OUT MENU DRAWER */}
            {user && (
                <>
                    <div
                        className={`drawer-backdrop ${menuOpen ? "open" : ""}`}
                        onClick={() => setMenuOpen(false)}
                    />
                    <aside className={`menu-drawer ${menuOpen ? "open" : ""}`}>
                        <div className="drawer-header">
                            <h3>Account & Navigation</h3>
                            <button className="close-drawer" onClick={() => setMenuOpen(false)}>✕</button>
                        </div>

                        <div className="drawer-user-info" onClick={() => { setMenuOpen(false); setActiveModal("profile"); }}>
                            {renderAvatarDisplay("normal")}
                            <div className="user-text-col">
                                <strong>{user.name}</strong>
                                <small>{user.email}</small>
                                <span className="role-tag">
                                    {user.role} {user.role === "NGO" && (user.ngoVerified ? "✓ Verified" : "• Pending")}
                                </span>
                            </div>
                        </div>

                        <div className="drawer-nav">
                            <div className="nav-item active" onClick={() => setMenuOpen(false)}>
                                <span>📋</span> Active Dashboard
                            </div>
                            <div className="nav-item" onClick={() => { setMenuOpen(false); setActiveModal("history"); }}>
                                <span>📜</span> Rescue Activity History
                            </div>
                            {user.role === "NGO" && !user.ngoVerified && (
                                <div className="nav-item highlight-nav" onClick={() => { setMenuOpen(false); setActiveModal("ngo-verify"); }}>
                                    <span>🛡️</span> Complete NGO Verification
                                </div>
                            )}
                            <div className="nav-item" onClick={() => { setMenuOpen(false); setActiveModal("profile"); }}>
                                <span>👤</span> Profile & Avatar
                            </div>
                            <div className="nav-item" onClick={() => { setMenuOpen(false); setActiveModal("security"); }}>
                                <span>🔒</span> Security & Password
                            </div>
                            <div className="nav-item" onClick={() => { setMenuOpen(false); refreshAll(); }}>
                                <span>🔄</span> Refresh Platform Data
                            </div>
                            <div className="nav-item" onClick={toggleTheme}>
                                <span>🌓</span> Theme: <b>{theme === "dark" ? "Dark" : "Light"}</b>
                            </div>
                        </div>

                        <div className="drawer-footer">
                            <button className="drawer-logout-btn" onClick={logout}>
                                Sign Out
                            </button>
                        </div>
                    </aside>
                </>
            )}

            {/* USER SETTINGS / MODALS */}
            {activeModal && (
                <div className="modal-backdrop" onClick={() => setActiveModal(null)}>
                    <div className="modal-card" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>
                                {activeModal === "profile" && "Profile & Avatar Customization"}
                                {activeModal === "history" && `${user.role} Rescue Activity History`}
                                {activeModal === "security" && "Security & Password Management"}
                                {activeModal === "ngo-verify" && "NGO Legal Verification"}
                            </h3>
                            <button className="close-drawer" onClick={() => setActiveModal(null)}>✕</button>
                        </div>

                        {/* NGO VERIFICATION ONBOARDING MODAL */}
                        {activeModal === "ngo-verify" && (
                            <div className="modal-body">
                                <p className="history-intro">
                                    To protect food safety and prevent misuse, NGOs must provide their Government Darpan ID or Society Registration Number to claim listings.
                                </p>

                                <form onSubmit={submitNgoVerification} className="security-form">
                                    <label>NGO Darpan Unique ID / Reg Number</label>
                                    <input
                                        type="text"
                                        placeholder="Example: TN/2021/0123456"
                                        value={darpanId}
                                        onChange={(e) => setDarpanId(e.target.value)}
                                        required
                                    />
                                    <small className="field-hint">
                                        Registered on ngodarpan.gov.in or under Indian Societies/Trusts Act.
                                    </small>

                                    <label>Authorized Representative Mobile (10-digits)</label>
                                    <input
                                        type="tel"
                                        pattern="[0-9]{10}"
                                        maxLength="10"
                                        placeholder="Example: 9876543210"
                                        value={ngoPhone}
                                        onChange={(e) => setNgoPhone(e.target.value.replace(/\D/g, ""))}
                                        required
                                    />

                                    <button className="primary submit-large" type="submit" disabled={ngoVerifyLoading}>
                                        {ngoVerifyLoading ? "Verifying Credentials..." : "Submit & Activate NGO Account"}
                                    </button>
                                </form>

                                {ngoVerifyMsg && (
                                    <div className="msg" role="status">
                                        {ngoVerifyMsg}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* PROFILE & AVATAR MODAL */}
                        {activeModal === "profile" && (
                            <div className="modal-body">
                                <div className="avatar-preview-row">
                                    {renderAvatarDisplay("large")}
                                    <div>
                                        <h4>{user.name}</h4>
                                        <p>{user.email}</p>
                                        <span className="role-tag">{user.role}</span>
                                    </div>
                                </div>

                                <label>Choose Avatar Icon</label>
                                <div className="avatar-preset-grid">
                                    {["🌱", "🍲", "🌾", "🦸", "🤝", "📦", "🛵", "❤️", "🌟", "✨"].map((emoji) => (
                                        <button
                                            key={emoji}
                                            type="button"
                                            className={`avatar-preset-btn ${customAvatar === emoji ? "selected" : ""}`}
                                            onClick={() => handleAvatarSelect(emoji)}
                                        >
                                            {emoji}
                                        </button>
                                    ))}
                                </div>

                                <label>Or Upload Custom Profile Picture</label>
                                <input type="file" accept="image/*" onChange={handleAvatarUpload} className="file-input" />

                                {customAvatar && (
                                    <button type="button" className="text-button remove-avatar-btn" onClick={handleRemoveAvatar}>
                                        Reset to Default Letter
                                    </button>
                                )}
                            </div>
                        )}

                        {/* HISTORY LOG MODAL */}
                        {activeModal === "history" && (
                            <div className="modal-body history-body">
                                <p className="history-intro">
                                    Displaying all verified donations and activities registered for your <b>{user.role}</b> account.
                                </p>

                                {userHistory.length === 0 ? (
                                    <div className="empty-history">No past activity records found for this account.</div>
                                ) : (
                                    <div className="history-list">
                                        {userHistory.map((item) => (
                                            <div key={item.id} className="history-row">
                                                <div>
                                                    <strong>{item.title}</strong>
                                                    <div className="history-meta">
                                                        <span>🍽 {item.quantity} meals</span>
                                                        <span>📍 {item.location}</span>
                                                    </div>
                                                </div>
                                                <div className="history-right">
                                                    <span className={`status-pill status-${String(item.status).toLowerCase()}`}>
                                                        {item.status}
                                                    </span>
                                                    <small>{new Date(item.time).toLocaleDateString([], { month: 'short', day: 'numeric' })}</small>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* SECURITY & PASSWORD MODAL */}
                        {activeModal === "security" && (
                            <div className="modal-body">
                                <form onSubmit={handleChangePasswordSubmit} className="security-form">
                                    <label>Current Password</label>
                                    <input
                                        type="password"
                                        value={oldPassword}
                                        onChange={(e) => setOldPassword(e.target.value)}
                                        placeholder="Enter your current password"
                                        required
                                    />

                                    <label>New Password</label>
                                    <input
                                        type="password"
                                        minLength={8}
                                        maxLength={72}
                                        value={newPasswordVal}
                                        onChange={(e) => setNewPasswordVal(e.target.value)}
                                        placeholder="Enter your new secure password"
                                        required
                                    />

                                    <PasswordRequirements password={newPasswordVal} />

                                    <label>Confirm New Password</label>
                                    <input
                                        type="password"
                                        minLength={8}
                                        maxLength={72}
                                        value={confirmNewPasswordVal}
                                        onChange={(e) => setConfirmNewPasswordVal(e.target.value)}
                                        placeholder="Re-enter your new password"
                                        required
                                    />

                                    <button className="primary submit-large" type="submit">
                                        Update Password
                                    </button>
                                </form>

                                {changePasswordMsg && (
                                    <div className="msg" role="status">
                                        {changePasswordMsg}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}

            <main>
                {!user ? (
                    showResetPassword ? (
                        <section className="login-card verification-card">
                            <div className="verification-hero" aria-hidden="true">
                                <div className="verification-icon">🔐</div>
                            </div>
                            <div className="eyebrow">ACCOUNT SECURITY</div>
                            <h2>Create a new password</h2>
                            {resetTokenChecking ? (
                                <p>Checking your password reset link...</p>
                            ) : resetPasswordMessage &&
                              (resetPasswordMessage.toLowerCase().includes("invalid") ||
                                  resetPasswordMessage.toLowerCase().includes("expired")) ? (
                                <>
                                    <p className="msg">{resetPasswordMessage}</p>
                                    <button type="button" className="primary submit-large" onClick={openForgotPassword}>
                                        Request a new link
                                    </button>
                                </>
                            ) : (
                                <>
                                    <p>Choose a new password for your SharePlate account.</p>
                                    <form onSubmit={resetPasswordSubmit}>
                                        <label>New password</label>
                                        <input
                                            type="password"
                                            minLength={8}
                                            maxLength={72}
                                            value={resetPassword}
                                            onChange={(event) => setResetPassword(event.target.value)}
                                            placeholder="Enter your new password"
                                            required
                                        />

                                        <PasswordRequirements password={resetPassword} />

                                        <label>Confirm new password</label>
                                        <input
                                            type="password"
                                            minLength={8}
                                            maxLength={72}
                                            value={resetConfirmPassword}
                                            onChange={(event) => setResetConfirmPassword(event.target.value)}
                                            placeholder="Re-enter your new password"
                                            required
                                        />
                                        <button
                                            className="primary submit-large"
                                            type="submit"
                                            disabled={resetPasswordLoading || resetTokenChecking}
                                        >
                                            {resetPasswordLoading ? "Updating password..." : "Reset Password"}
                                        </button>
                                    </form>
                                    {resetPasswordMessage && (
                                        <div className="msg" role="status">
                                            {resetPasswordMessage}
                                        </div>
                                    )}
                                    <div className="auth-footer-nav">
                                        <button type="button" className="text-button" onClick={openLogin}>
                                            ← Back to sign in
                                        </button>
                                    </div>
                                </>
                            )}
                        </section>
                    ) : showForgotPassword ? (
                        <section className="login-card verification-card">
                            <div className="verification-hero" aria-hidden="true">
                                <div className="verification-icon">✉</div>
                            </div>
                            <div className="eyebrow">ACCOUNT RECOVERY</div>
                            <h2>Forgot your password?</h2>
                            <p>
                                Enter the email address linked to your SharePlate account. If an account
                                exists, we'll send you a secure password reset link.
                            </p>
                            <form onSubmit={requestPasswordReset}>
                                <label>Email</label>
                                <input
                                    type="email"
                                    value={forgotPasswordEmail}
                                    onChange={(event) => setForgotPasswordEmail(event.target.value)}
                                    placeholder="Enter your email"
                                    required
                                />
                                <button
                                    className="primary submit-large"
                                    type="submit"
                                    disabled={forgotPasswordLoading}
                                >
                                    {forgotPasswordLoading ? "Sending reset link..." : "Send Reset Link"}
                                </button>
                            </form>
                            {forgotPasswordMessage && (
                                <div className="msg" role="status">
                                    {forgotPasswordMessage}
                                </div>
                            )}
                            <div className="auth-footer-nav">
                                <button type="button" className="text-button" onClick={openLogin}>
                                    ← Back to sign in
                                </button>
                            </div>
                        </section>
                    ) : showVerification ? (
                        <section className="login-card verification-card">
                            <div className="verification-hero" aria-hidden="true">
                                <div className="verification-icon">✉</div>
                            </div>
                            <div className="eyebrow">ONE SMALL STEP</div>
                            <h2>Verify your email</h2>
                            <p>
                                We've sent a 6-digit verification code to{" "}
                                <strong className="verification-email">{verificationEmail}</strong>
                            </p>
                            <form onSubmit={verifyEmail} className="verification-form">
                                <div
                                    className="otp-grid"
                                    onPaste={(event) => {
                                        event.preventDefault();
                                        const pasted = event.clipboardData
                                            .getData("text")
                                            .replace(/\D/g, "")
                                            .slice(0, 6);
                                        if (!pasted) return;
                                        const next = ["", "", "", "", "", ""];
                                        pasted.split("").forEach((digit, index) => {
                                            next[index] = digit;
                                        });
                                        setVerificationCode(next);
                                        document
                                            .getElementById(`verification-${Math.min(pasted.length, 5)}`)
                                            ?.focus();
                                    }}
                                >
                                    {verificationCode.map((digit, index) => (
                                        <input
                                            key={index}
                                            id={`verification-${index}`}
                                            className="otp-input"
                                            type="text"
                                            inputMode="numeric"
                                            autoComplete={index === 0 ? "one-time-code" : "off"}
                                            maxLength="1"
                                            value={digit}
                                            onChange={(event) => updateVerificationDigit(index, event.target.value)}
                                            onKeyDown={(event) => handleVerificationKeyDown(event, index)}
                                            aria-label={`Verification digit ${index + 1}`}
                                        />
                                    ))}
                                </div>
                                <button
                                    className="primary submit-large"
                                    type="submit"
                                    disabled={verificationLoading}
                                >
                                    {verificationLoading ? "Verifying..." : "Verify Email"}
                                </button>
                            </form>
                            {verificationMessage && (
                                <div className="msg" role="status">
                                    {verificationMessage}
                                </div>
                            )}
                            <div className="verification-resend">
                                <span>Didn't receive the code?</span>
                                <button
                                    type="button"
                                    onClick={resendVerification}
                                    disabled={resendLoading || resendCooldown > 0}
                                >
                                    {resendLoading
                                        ? "Sending..."
                                        : resendCooldown > 0
                                        ? `Resend in ${resendCooldown}s`
                                        : "Resend code"}
                                </button>
                            </div>
                            <div className="auth-footer-nav">
                                <button type="button" className="text-button" onClick={openLogin}>
                                    ← Back to sign in
                                </button>
                            </div>
                        </section>
                    ) : showSignup ? (
                        <section className="login-card">
                            <div className="eyebrow">JOIN SHAREPLATE</div>
                            <h2>Create your account</h2>
                            <p>Join SharePlate and help rescue surplus food.</p>

                            <form onSubmit={register}>
                                <label>Full name</label>
                                <input
                                    type="text"
                                    value={signupName}
                                    onChange={(event) => setSignupName(event.target.value)}
                                    placeholder="Enter your name"
                                    required
                                />

                                <label>Email</label>
                                <input
                                    type="email"
                                    value={signupEmail}
                                    onChange={(event) => setSignupEmail(event.target.value)}
                                    placeholder="Enter your email"
                                    required
                                />

                                <label>Password</label>
                                <input
                                    type="password"
                                    value={signupPassword}
                                    onChange={(event) => setSignupPassword(event.target.value)}
                                    placeholder="Create a password"
                                    required
                                />

                                <PasswordRequirements password={signupPassword} />

                                <label>Confirm password</label>
                                <input
                                    type="password"
                                    value={signupConfirmPassword}
                                    onChange={(event) => setSignupConfirmPassword(event.target.value)}
                                    placeholder="Re-enter your password"
                                    required
                                />

                                <label>I want to join as</label>
                                <select
                                    value={signupRole}
                                    onChange={(event) => setSignupRole(event.target.value)}
                                    required
                                >
                                    <option value="DONOR">Donor</option>
                                    <option value="NGO">NGO</option>
                                    <option value="VOLUNTEER">Volunteer</option>
                                </select>

                                <button className="primary submit-large" type="submit" disabled={signupLoading}>
                                    {signupLoading ? "Creating account..." : "Create Account"}
                                </button>
                            </form>

                            {signupMessage && <div className="msg">{signupMessage}</div>}

                            <div className="auth-footer-nav">
                                <span>Already have an account?</span>
                                <button type="button" className="text-link-button" onClick={openLogin}>
                                    Sign in
                                </button>
                            </div>
                        </section>
                    ) : (
                        <section className="login-card">
                            <div className="eyebrow">WELCOME BACK</div>
                            <h2>Welcome to SharePlate</h2>
                            <p>Sign in to continue rescue operations.</p>

                            <form onSubmit={login}>
                                <label>Email</label>
                                <input
                                    type="email"
                                    value={email}
                                    onChange={(event) => setEmail(event.target.value)}
                                    placeholder="Enter your email"
                                    required
                                />

                                <div className="password-header-row">
                                    <label>Password</label>
                                    <button type="button" className="inline-forgot-link" onClick={openForgotPassword}>
                                        Forgot password?
                                    </button>
                                </div>
                                <input
                                    type="password"
                                    value={password}
                                    onChange={(event) => setPassword(event.target.value)}
                                    placeholder="Enter your password"
                                    required
                                />

                                <button className="primary submit-large" type="submit" disabled={loading}>
                                    {loading ? "Signing in..." : "Sign In"}
                                </button>
                            </form>

                            {loginMessage && <div className="msg">{loginMessage}</div>}

                            <div className="auth-footer-nav">
                                <span>Don't have an account?</span>
                                <button type="button" className="text-link-button" onClick={openSignup}>
                                    Create an account
                                </button>
                            </div>
                        </section>
                    )
                ) : (
                    <>
                        {/* HERO BANNER WITH METRICS */}
                        <section className="hero modern-hero">
                            <div>
                                <div className="eyebrow">PLATFORM TELEMETRY</div>
                                <h2>Rescue more. Waste less.</h2>
                                <p>
                                    Connecting surplus food directly with verified organizations and volunteer drivers across the region.
                                </p>
                            </div>

                            <div className="metrics-strip">
                                <div className="metric-box">
                                    <span className="metric-value">{stats.totalMeals}</span>
                                    <span className="metric-label">Meals Listed</span>
                                </div>
                                <div className="metric-box">
                                    <span className="metric-value">{stats.activeRescues}</span>
                                    <span className="metric-label">In Rescue</span>
                                </div>
                                <div className="metric-box highlight">
                                    <span className="metric-value">{stats.deliveredCount}</span>
                                    <span className="metric-label">Delivered</span>
                                </div>
                            </div>
                        </section>

                        {user.role === "DONOR" && (
                            <>
                                <section className="form-card">
                                    <div className="section-heading">
                                        <div>
                                            <div className="eyebrow">DONOR CONSOLE</div>
                                            <h2>Post Surplus Food</h2>
                                        </div>
                                    </div>

                                    <form onSubmit={postFood}>
                                        <div className="form-split">
                                            <div>
                                                <label>Food name</label>
                                                <input
                                                    value={foodName}
                                                    onChange={(event) => setFoodName(event.target.value)}
                                                    placeholder="Example: Vegetable Rice & Dal"
                                                    required
                                                />
                                            </div>
                                            <div>
                                                <label>Quantity in meals</label>
                                                <input
                                                    type="number"
                                                    min="1"
                                                    value={quantity}
                                                    onChange={(event) => setQuantity(event.target.value)}
                                                    placeholder="Example: 25"
                                                    required
                                                />
                                            </div>
                                        </div>

                                        <div className="form-split">
                                            <div>
                                                <label>Pickup location</label>
                                                <input
                                                    value={location}
                                                    onChange={(event) => setLocation(event.target.value)}
                                                    placeholder="Example: Trichy Main Kitchen"
                                                    required
                                                />
                                            </div>
                                            <div>
                                                <label>Pickup deadline</label>
                                                <input
                                                    type="datetime-local"
                                                    value={pickupDeadline}
                                                    min={getDeadlineBounds().min}
                                                    max={getDeadlineBounds().max}
                                                    onChange={(event) => {
                                                        setPickupDeadline(event.target.value);
                                                        const error = validatePickupDeadline(event.target.value);
                                                        if (error) setMsg(error);
                                                        else if (msg) setMsg("");
                                                    }}
                                                    required
                                                />
                                            </div>
                                        </div>

                                        <small className="field-hint">
                                            Choose a pickup time at least 30 minutes from now and within the next 7 days.
                                        </small>

                                        <label>Description</label>
                                        <textarea
                                            value={description}
                                            onChange={(event) => setDescription(event.target.value)}
                                            placeholder="Describe the items, containers, or packaging..."
                                        />

                                        <label>Safety & Storage details</label>
                                        <textarea
                                            value={safetyDetails}
                                            onChange={(event) => setSafetyDetails(event.target.value)}
                                            placeholder="Prepared time, temperature requirements, allergens..."
                                        />

                                        <button className="primary submit-large" type="submit" disabled={posting}>
                                            {posting ? "Publishing Donation..." : "Publish Food Listing"}
                                        </button>
                                    </form>
                                </section>

                                {donorOwnTasks.length > 0 && (
                                    <section className="form-card">
                                        <div className="section-heading">
                                            <div>
                                                <div className="eyebrow">HANDOVER SECURITY</div>
                                                <h2>Your Pickup Codes</h2>
                                            </div>
                                        </div>

                                        <div className="grid">
                                            {donorOwnTasks.map((task) => {
                                                const listing = listings.find(
                                                    (item) => Number(item.id) === Number(task.listingId)
                                                );
                                                const code = pickupCodes[task.id];

                                                return (
                                                    <article key={task.id} className="code-card">
                                                        <div className="top">
                                                            <h3>{listing?.foodName || `Pickup Task #${task.id}`}</h3>
                                                            <span className="status-badge">{task.status}</span>
                                                        </div>

                                                        <div className="details listing-details">
                                                            <div>Task #{task.id}</div>
                                                            <div>Listing #{task.listingId}</div>
                                                        </div>

                                                        <div className="verification-box pickup-box">
                                                            <div>
                                                                <strong>Pickup Handover Code</strong>
                                                                <small>Share this code with the volunteer driver upon food collection.</small>
                                                            </div>

                                                            <div className="code-interactive-group">
                                                                <div className="code-value">
                                                                    {code || (codeLoading[`pickup-${task.id}`] ? "Loading..." : "Unavailable")}
                                                                </div>
                                                                {code && (
                                                                    <button
                                                                        type="button"
                                                                        className="copy-chip"
                                                                        onClick={() => copyCode(code, `pickup-${task.id}`)}
                                                                    >
                                                                        {copiedCodeKey === `pickup-${task.id}` ? "Copied! ✓" : "Copy Code"}
                                                                    </button>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </article>
                                                );
                                            })}
                                        </div>
                                    </section>
                                )}
                            </>
                        )}

                        {user.role === "NGO" && (
                            <section className="form-card">
                                <div className="section-heading">
                                    <div>
                                        <div className="eyebrow">NGO COORDINATION</div>
                                        <h2>Coordinate Food Rescue</h2>
                                    </div>
                                </div>

                                <p>Claim available surplus listings and assign verified volunteers for collection.</p>

                                <h3>Available Registered Volunteers</h3>

                                {volunteers.length === 0 ? (
                                    <p className="empty-sub">No volunteers registered yet.</p>
                                ) : (
                                    <div className="grid">
                                        {volunteers.map((volunteer) => (
                                            <article key={volunteer.id} className="volunteer-card">
                                                <div className="avatar-chip mini">{volunteer.name.charAt(0)}</div>
                                                <div>
                                                    <h3>{volunteer.name}</h3>
                                                    <div className="details mini-details">
                                                        <div>ID: #{volunteer.id}</div>
                                                        <div>{volunteer.email}</div>
                                                        <div className={`verified-state ${volunteer.verified ? "yes" : "no"}`}>
                                                            {volunteer.verified ? "Verified Volunteer ✓" : "Pending Verification"}
                                                        </div>
                                                    </div>
                                                </div>
                                            </article>
                                        ))}
                                    </div>
                                )}

                                {ngoTasks.length > 0 && (
                                    <div className="task-code-list">
                                        <h3>NGO Delivery Codes</h3>

                                        <div className="grid">
                                            {ngoTasks
                                                .filter((task) => {
                                                    const listing = listings.find(
                                                        (item) => Number(item.id) === Number(task.listingId)
                                                    );
                                                    return listing && Number(listing.claimedByNgoId) === Number(user.id);
                                                })
                                                .map((task) => {
                                                    const listing = listings.find(
                                                        (item) => Number(item.id) === Number(task.listingId)
                                                    );
                                                    const assignedVolunteer = getVolunteer(task.volunteerId);
                                                    const code = deliveryCodes[task.id];

                                                    return (
                                                        <article key={task.id} className="code-card">
                                                            <div className="top">
                                                                <h3>{listing?.foodName || `Task #${task.id}`}</h3>
                                                                <span className="status-badge">{task.status}</span>
                                                            </div>

                                                            <div className="details">
                                                                <div>Volunteer: <b>{assignedVolunteer ? assignedVolunteer.name : task.volunteerId}</b></div>
                                                                <div>Task #{task.id}</div>
                                                            </div>

                                                            <div className="verification-box delivery-box">
                                                                <div>
                                                                    <strong>Delivery Verification Code</strong>
                                                                    <small>Give this code to the volunteer when meals arrive.</small>
                                                                </div>

                                                                <div className="code-interactive-group">
                                                                    <div className="code-value">
                                                                        {code || (codeLoading[`delivery-${task.id}`] ? "Loading..." : "Unavailable")}
                                                                    </div>
                                                                    {code && (
                                                                        <button
                                                                            type="button"
                                                                            className="copy-chip"
                                                                            onClick={() => copyCode(code, `delivery-${task.id}`)}
                                                                        >
                                                                            {copiedCodeKey === `delivery-${task.id}` ? "Copied! ✓" : "Copy Code"}
                                                                        </button>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        </article>
                                                    );
                                                })}
                                        </div>
                                    </div>
                                )}
                            </section>
                        )}

                        {user.role === "VOLUNTEER" && (
                            <section className="form-card">
                                <div className="section-heading">
                                    <div>
                                        <div className="eyebrow">VOLUNTEER MISSIONS</div>
                                        <h2>My Pickup & Delivery Tasks</h2>
                                    </div>
                                </div>

                                {taskMessage && <div className="msg">{taskMessage}</div>}

                                {tasks.length === 0 ? (
                                    <p className="empty-sub">No pickup tasks currently assigned to you.</p>
                                ) : (
                                    <div className="grid">
                                        {tasks.map((task) => (
                                            <article key={task.id} className="task-card">
                                                <div className="top">
                                                    <h3>Pickup Task #{task.id}</h3>
                                                    <span className={`status-pill status-${String(task.status).toLowerCase()}`}>
                                                        {task.status}
                                                    </span>
                                                </div>

                                                <div className="details">
                                                    <div>Listing ID: <b>#{task.listingId}</b></div>
                                                    <div>Collected at: <b>{task.collectedAt ? new Date(task.collectedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "Pending"}</b></div>
                                                    <div>Delivered at: <b>{task.deliveredAt ? new Date(task.deliveredAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "Pending"}</b></div>
                                                </div>

                                                {task.status === "ASSIGNED" && (
                                                    <div className="action-box">
                                                        <label>Step 1: Enter Donor Pickup Code</label>
                                                        <input
                                                            type="text"
                                                            maxLength="6"
                                                            inputMode="numeric"
                                                            value={handoverCodes[task.id] || ""}
                                                            onChange={(event) => updateHandoverCode(task.id, event.target.value)}
                                                            placeholder="Enter 6-digit pickup code"
                                                        />
                                                        <button className="primary submit-large" onClick={() => collectTask(task.id)}>
                                                            Confirm & Mark Collected
                                                        </button>
                                                    </div>
                                                )}

                                                {task.status === "COLLECTED" && (
                                                    <div className="action-box">
                                                        <label>Step 2: Enter NGO Delivery Code</label>
                                                        <input
                                                            type="text"
                                                            maxLength="6"
                                                            inputMode="numeric"
                                                            value={handoverCodes[task.id] || ""}
                                                            onChange={(event) => updateHandoverCode(task.id, event.target.value)}
                                                            placeholder="Enter 6-digit delivery code"
                                                        />
                                                        <button className="primary submit-large" onClick={() => deliverTask(task.id)}>
                                                            Confirm & Mark Delivered
                                                        </button>
                                                    </div>
                                                )}

                                                {task.status === "DELIVERED" && (
                                                    <div className="success-box">
                                                        Mission complete! Food safely delivered.
                                                    </div>
                                                )}
                                            </article>
                                        ))}
                                    </div>
                                )}
                            </section>
                        )}

                        {msg && <div className="msg">{msg}</div>}

                        {/* LIVE SEARCH & FILTER CONTROLS */}
                        <div className="controls-bar">
                            <div className="search-wrap">
                                <span className="search-icon">🔍</span>
                                <input
                                    type="text"
                                    placeholder="Search food items or location..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                                {searchTerm && (
                                    <button className="clear-search" onClick={() => setSearchTerm("")}>✕</button>
                                )}
                            </div>

                            <div className="filter-tabs">
                                {["ALL", "AVAILABLE", "CLAIMED", "DELIVERED"].map((tab) => (
                                    <button
                                        key={tab}
                                        type="button"
                                        className={`tab-btn ${statusFilter === tab ? "active" : ""}`}
                                        onClick={() => setStatusFilter(tab)}
                                    >
                                        {tab.charAt(0) + tab.slice(1).toLowerCase()}
                                    </button>
                                ))}
                            </div>

                            <button className="refresh-pill" onClick={refreshAll}>
                                <span>↻</span> Refresh
                            </button>
                        </div>

                        {/* LISTINGS GRID */}
                        <section className="grid">
                            {filteredListings.length ? (
                                filteredListings.map((listing) => {
                                    const existingTask = getTaskForListing(listing.id);
                                    const assignedVolunteer = existingTask
                                        ? getVolunteer(existingTask.volunteerId)
                                        : null;

                                    const listingTask =
                                        donorTasks.find((task) => Number(task.listingId) === Number(listing.id)) ||
                                        ngoTasks.find((task) => Number(task.listingId) === Number(listing.id)) ||
                                        tasks.find((task) => Number(task.listingId) === Number(listing.id));
                                    const isCompleted = listingTask?.status === "DELIVERED";
                                    const isOwnDonorListing =
                                        user.role === "DONOR" && Number(listing.donorId) === Number(user.id);

                                    return (
                                        <article
                                            key={listing.id}
                                            className={`listing-card modern-card ${
                                                isCompleted
                                                    ? "listing-completed"
                                                    : `status-${String(listing.status || "").toLowerCase()}`
                                            }`}
                                        >
                                            <div className="listing-card-glow" />
                                            <div className="top listing-top">
                                                <div>
                                                    <div className="listing-kicker">
                                                        {isCompleted ? "RESCUE COMPLETE" : "SURPLUS FOOD"}
                                                    </div>
                                                    <h3>{listing.foodName}</h3>
                                                </div>
                                                <div className="status-stack">
                                                    <span
                                                        className={`status-pill status-${String(
                                                            isCompleted ? "DELIVERED" : listing.status || ""
                                                        ).toLowerCase()}`}
                                                    >
                                                        <i aria-hidden="true">
                                                            {isCompleted ? "✓" : listing.status === "AVAILABLE" ? "●" : "↗"}
                                                        </i>
                                                        {isCompleted ? "DELIVERED" : listing.status}
                                                    </span>
                                                    {!isCompleted && getUrgencyBadge(listing.pickupDeadline)}
                                                </div>
                                            </div>

                                            <p className="listing-description">
                                                {listing.description || "No specific storage details provided."}
                                            </p>

                                            <div className="details meta-card">
                                                <div>
                                                    <span>🍽</span> <strong>{listing.quantity}</strong> meals prepared
                                                </div>
                                                <div>
                                                    <span>📍</span> {listing.location}
                                                </div>
                                                <div>
                                                    <span>⏰</span> Deadline: {new Date(listing.pickupDeadline).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                                                </div>
                                            </div>

                                            {isCompleted && (
                                                <div className="impact-seal" role="status" aria-label="Food successfully delivered">
                                                    <div className="seal-mark">✓</div>
                                                    <div className="seal-copy">
                                                        <strong>Successfully delivered</strong>
                                                        <span>
                                                            {user?.role === "DONOR" && "Your surplus food reached families in need."}
                                                            {user?.role === "NGO" && "Great coordination! You turned surplus into nourishment."}
                                                            {user?.role === "VOLUNTEER" && "Direct impact delivered! Thank you for going the distance."}
                                                        </span>
                                                    </div>
                                                </div>
                                            )}

                                            {isOwnDonorListing && listingTask && !isCompleted && (
                                                <div className="listing-progress">
                                                    <div className="progress-label">
                                                        <span>Rescue in progress</span>
                                                        <strong>{listingTask.status}</strong>
                                                    </div>
                                                    <div className="progress-track">
                                                        <span
                                                            className="progress-fill"
                                                            style={{
                                                                width:
                                                                    listingTask.status === "COLLECTED"
                                                                        ? "78%"
                                                                        : listingTask.status === "ASSIGNED"
                                                                        ? "52%"
                                                                        : "34%",
                                                            }}
                                                        />
                                                    </div>
                                                    <small>Your surplus food is actively being rescued.</small>
                                                </div>
                                            )}

                                            {listing.status === "AVAILABLE" && user.role === "NGO" && (
                                                <button
                                                    className="primary submit-large"
                                                    onClick={() => handleClaimClick(listing.id)}
                                                >
                                                    Claim for NGO
                                                </button>
                                            )}

                                            {listing.status === "CLAIMED" &&
                                                user.role === "NGO" &&
                                                (existingTask ? (
                                                    <div className="assignment-box">
                                                        <h4>✅ Volunteer Assigned</h4>
                                                        <p>Volunteer: <b>{assignedVolunteer ? assignedVolunteer.name : "Assigned"}</b></p>
                                                        <p>Volunteer ID: #{existingTask.volunteerId}</p>
                                                        <button disabled className="assigned-pill">
                                                            ✓ Assigned to Delivery
                                                        </button>
                                                    </div>
                                                ) : (
                                                    <div className="assignment-box">
                                                        <h4>Assign Field Volunteer</h4>
                                                        <select
                                                            value={selectedVolunteers[listing.id] || ""}
                                                            onChange={(event) =>
                                                                selectVolunteer(listing.id, event.target.value)
                                                            }
                                                        >
                                                            <option value="">Select a volunteer</option>
                                                            {volunteers.map((volunteer) => (
                                                                <option key={volunteer.id} value={volunteer.id}>
                                                                    {volunteer.name} (ID #{volunteer.id})
                                                                </option>
                                                            ))}
                                                        </select>
                                                        <button
                                                            className="primary submit-large"
                                                            onClick={() => assignVolunteer(listing.id)}
                                                            disabled={assigningListing === listing.id}
                                                        >
                                                            {assigningListing === listing.id ? "Assigning..." : "Assign Volunteer"}
                                                        </button>
                                                    </div>
                                                ))}
                                        </article>
                                    );
                                })
                            ) : (
                                <div className="empty modern-empty">
                                    <span className="empty-icon">🍃</span>
                                    <h3>No food listings match your criteria</h3>
                                    <p>Check back shortly or post a surplus food listing to get started.</p>
                                </div>
                            )}
                        </section>
                    </>
                )}
            </main>
        </div>
    );
}

createRoot(document.getElementById("root")).render(<App />);