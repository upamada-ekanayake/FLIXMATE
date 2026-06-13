'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../store/authStore';
import { Film, User, LogOut, Shield, Menu, X, Sun, Moon } from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuthStore();
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');

  // Load and apply theme
  useEffect(() => {
    const savedTheme = localStorage.getItem('flixmate_theme') as 'dark' | 'light';
    if (savedTheme) {
      setTheme(savedTheme);
      if (savedTheme === 'light') {
        document.body.classList.add('light-theme');
      }
    }
  }, []);

  const toggleTheme = () => {
    if (theme === 'dark') {
      document.body.classList.add('light-theme');
      localStorage.setItem('flixmate_theme', 'light');
      setTheme('light');
    } else {
      document.body.classList.remove('light-theme');
      localStorage.setItem('flixmate_theme', 'dark');
      setTheme('dark');
    }
  };

  const handleLogout = () => {
    logout();
    router.push('/');
  };

  return (
    <nav className="glass-navbar sticky top-0 z-50 w-full transition-all duration-300">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center">
            <Link href="/" className="flex items-center space-x-2 text-red-600 font-extrabold text-2xl tracking-wider hover:opacity-90">
              <Film className="h-8 w-8" />
              <span>FLIXMATE</span>
            </Link>
          </div>

          {/* Desktop Nav Links */}
          <div className="hidden md:flex items-center space-x-6">
            <Link href="/" className="text-gray-300 hover:text-white transition font-medium">
              Home
            </Link>
            {user && (
              <Link href="/dashboard" className="text-gray-300 hover:text-white transition font-medium">
                My Tickets
              </Link>
            )}
            {user?.role === 'ROLE_ADMIN' && (
              <Link href="/admin" className="flex items-center space-x-1 text-red-500 hover:text-red-400 transition font-medium">
                <Shield className="h-4 w-4" />
                <span>Admin Panel</span>
              </Link>
            )}

            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-full hover:bg-gray-800 light-theme:hover:bg-gray-200 transition text-gray-400 hover:text-white"
              aria-label="Toggle Theme"
            >
              {theme === 'dark' ? <Sun className="h-5 w-5 text-yellow-500" /> : <Moon className="h-5 w-5 text-indigo-600" />}
            </button>

            {/* Auth Buttons */}
            {user ? (
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-2 text-sm text-gray-300">
                  <User className="h-4 w-4 text-red-500" />
                  <span className="font-semibold">{user.firstName}</span>
                </div>
                <button
                  onClick={handleLogout}
                  className="flex items-center space-x-1 px-4 py-2 rounded-md bg-gray-800 hover:bg-red-700 hover:text-white text-gray-300 transition text-sm font-semibold"
                >
                  <LogOut className="h-4 w-4" />
                  <span>Logout</span>
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-3">
                <Link href="/login" className="px-4 py-2 text-sm text-gray-300 hover:text-white transition font-semibold">
                  Sign In
                </Link>
                <Link
                  href="/register"
                  className="px-4 py-2 text-sm bg-red-600 hover:bg-red-700 text-white rounded-md transition font-semibold shadow-lg shadow-red-600/30"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>

          {/* Mobile Menu Button */}
          <div className="md:hidden flex items-center space-x-3">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-full text-gray-400 hover:text-white"
              aria-label="Toggle Theme"
            >
              {theme === 'dark' ? <Sun className="h-5 w-5 text-yellow-500" /> : <Moon className="h-5 w-5 text-indigo-600" />}
            </button>
            <button
              onClick={() => setIsOpen(!isOpen)}
              className="p-2 rounded-md text-gray-400 hover:text-white focus:outline-none"
            >
              {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Open Drawer */}
      {isOpen && (
        <div className="md:hidden glass-panel px-2 pt-2 pb-4 space-y-1 sm:px-3 border-t">
          <Link
            href="/"
            onClick={() => setIsOpen(false)}
            className="block px-3 py-2 rounded-md text-base font-medium text-gray-300 hover:text-white hover:bg-gray-800"
          >
            Home
          </Link>
          {user && (
            <Link
              href="/dashboard"
              onClick={() => setIsOpen(false)}
              className="block px-3 py-2 rounded-md text-base font-medium text-gray-300 hover:text-white hover:bg-gray-800"
            >
              My Tickets
            </Link>
          )}
          {user?.role === 'ROLE_ADMIN' && (
            <Link
              href="/admin"
              onClick={() => setIsOpen(false)}
              className="block px-3 py-2 rounded-md text-base font-medium text-red-500 hover:bg-gray-800"
            >
              Admin Panel
            </Link>
          )}
          
          <div className="pt-4 pb-2 border-t border-gray-700">
            {user ? (
              <div className="px-3">
                <p className="text-base font-medium text-white">{user.firstName} {user.lastName}</p>
                <p className="text-sm font-medium text-gray-400">{user.email}</p>
                <button
                  onClick={() => {
                    setIsOpen(false);
                    handleLogout();
                  }}
                  className="mt-3 w-full flex items-center justify-center space-x-2 px-4 py-2 bg-red-600 hover:bg-red-700 rounded-md text-white font-semibold transition"
                >
                  <LogOut className="h-4 w-4" />
                  <span>Logout</span>
                </button>
              </div>
            ) : (
              <div className="px-3 space-y-2">
                <Link
                  href="/login"
                  onClick={() => setIsOpen(false)}
                  className="block w-full text-center px-4 py-2 bg-gray-800 text-gray-300 hover:text-white rounded-md font-semibold transition"
                >
                  Sign In
                </Link>
                <Link
                  href="/register"
                  onClick={() => setIsOpen(false)}
                  className="block w-full text-center px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-md font-semibold transition"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
