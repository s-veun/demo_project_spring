"use client";

import { useEffect, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export default function SocialLoginPage() {
  const [profile, setProfile] = useState<any>(null);
  const [theme, setTheme] = useState<"light" | "dark">("light");

  useEffect(() => {
    const storedAccessToken = localStorage.getItem("accessToken");
    if (storedAccessToken) {
      loadProfile(storedAccessToken);
    }
  }, []);

  const continueWith = async (provider: "google" | "facebook") => {
    const res = await fetch(`${API_BASE}/api/v1/auth/oauth2/${provider}`);
    const data = await res.json();
    const authUrl = data?.data?.authorizationUri;
    if (authUrl) {
      window.location.href = `${API_BASE}${authUrl}`;
    }
  };

  const loadProfile = async (token: string) => {
    const res = await fetch(`${API_BASE}/api/v1/user/profile`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      const body = await res.json();
      setProfile(body.data);
    }
  };

  const onLogout = async () => {
    const accessToken = localStorage.getItem("accessToken");
    const refreshToken = localStorage.getItem("refreshToken");

    await fetch(`${API_BASE}/api/v1/auth/logout`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: accessToken ? `Bearer ${accessToken}` : "",
      },
      body: JSON.stringify({ refreshToken }),
    });

    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    setProfile(null);
  };

  return (
    <main className={theme === "dark" ? "dark" : ""}>
      <section className="min-h-screen flex items-center justify-center bg-white dark:bg-slate-900 transition-all">
        <div className="w-full max-w-md p-8 rounded-2xl shadow-xl bg-white dark:bg-slate-800">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-semibold text-slate-900 dark:text-white">Social Authentication</h1>
            <button
              onClick={() => setTheme(theme === "light" ? "dark" : "light")}
              className="text-sm px-3 py-1 rounded border border-slate-300 dark:border-slate-600"
            >
              {theme === "light" ? "Dark" : "Light"}
            </button>
          </div>

          {!profile ? (
            <div className="space-y-3">
              <button
                onClick={() => continueWith("google")}
                className="w-full rounded-lg px-4 py-3 bg-white border border-slate-300 hover:bg-slate-50"
              >
                Continue with Google
              </button>
              <button
                onClick={() => continueWith("facebook")}
                className="w-full rounded-lg px-4 py-3 bg-[#1877F2] text-white hover:opacity-90"
              >
                Continue with Facebook
              </button>
            </div>
          ) : (
            <div className="space-y-3 text-slate-900 dark:text-slate-100">
              <p className="font-medium">{profile.firstName} {profile.lastName}</p>
              <p>{profile.email}</p>
              <p className="text-sm opacity-80">Provider: {profile.provider}</p>
              <button
                onClick={onLogout}
                className="w-full rounded-lg px-4 py-3 bg-slate-900 text-white dark:bg-white dark:text-slate-900"
              >
                Logout User
              </button>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}

