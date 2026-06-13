'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '../../store/authStore';
import api from '../../services/api';
import { Film, Mail, Lock, ArrowRight } from 'lucide-react';
import { motion } from 'framer-motion';

export default function LoginPage() {
  const { login } = useAuthStore();
  const router = useRouter();
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await api.post('/auth/login', { email, password });
      login(res.data);
      router.push('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-zinc-950 px-4 py-12 sm:px-6 lg:px-8">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass-panel w-full max-w-md space-y-8 rounded-2xl p-8 border border-zinc-800"
      >
        <div className="text-center">
          <Film className="mx-auto h-12 w-12 text-red-600" />
          <h2 className="mt-6 text-3xl font-extrabold tracking-tight text-white">
            Welcome Back
          </h2>
          <p className="mt-2 text-sm text-gray-400">
            Or{' '}
            <Link href="/register" className="font-semibold text-red-500 hover:text-red-400">
              create a new account
            </Link>
          </p>
        </div>

        {error && (
          <div className="rounded-md bg-red-900/30 border border-red-500/50 p-4 text-sm text-red-400 text-center">
            {error}
          </div>
        )}

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4 rounded-md">
            <div>
              <label htmlFor="email-address" className="sr-only">Email address</label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                  <Mail className="h-5 w-5 text-gray-500" />
                </div>
                <input
                  id="email-address"
                  name="email"
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="block w-full rounded-lg bg-zinc-900 border border-zinc-800 px-3 py-3 pl-10 text-white placeholder-gray-500 outline-none transition focus:border-red-500 focus:ring-1 focus:ring-red-500 sm:text-sm"
                  placeholder="Email address"
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="sr-only">Password</label>
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                  <Lock className="h-5 w-5 text-gray-500" />
                </div>
                <input
                  id="password"
                  name="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="block w-full rounded-lg bg-zinc-900 border border-zinc-800 px-3 py-3 pl-10 text-white placeholder-gray-500 outline-none transition focus:border-red-500 focus:ring-1 focus:ring-red-500 sm:text-sm"
                  placeholder="Password"
                />
              </div>
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative flex w-full justify-center rounded-lg bg-red-600 hover:bg-red-700 py-3 text-sm font-semibold text-white outline-none transition hover:shadow-lg hover:shadow-red-600/30 disabled:opacity-50"
            >
              {loading ? 'Signing in...' : 'Sign In'}
              {!loading && <ArrowRight className="ml-2 h-4 w-4 mt-0.5" />}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}
