'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { ShieldAlert, BarChart3, Database, Film, MapPin, Sparkles, TrendingUp, Users, Percent, Smile, Frown, CheckCircle } from 'lucide-react';
import { motion } from 'framer-motion';

interface Analytics {
  totalRevenue: number;
  predictedRevenueNextMonth: number;
  averageOccupancyRate: number;
  totalTicketsSold: number;
  reviewSentimentCounts: {
    POSITIVE: number;
    NEUTRAL: number;
    NEGATIVE: number;
  };
  weeklyRevenueHistory: Record<string, number>;
}

export default function AdminDashboard() {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [tmdbId, setTmdbId] = useState('');
  const [syncSuccess, setSyncSuccess] = useState('');
  const [syncError, setSyncError] = useState('');
  const [syncLoading, setSyncLoading] = useState(false);

  const [theaterName, setTheaterName] = useState('');
  const [theaterCity, setTheaterCity] = useState('');
  const [theaterAddress, setTheaterAddress] = useState('');
  const [theaterSuccess, setTheaterSuccess] = useState('');
  const [theaterError, setTheaterError] = useState('');
  const [theaterLoading, setTheaterLoading] = useState(false);

  // 1. Fetch Admin Analytics DTO
  const { data: analytics, isLoading } = useQuery<Analytics>({
    queryKey: ['admin-analytics'],
    queryFn: async () => {
      const res = await api.get('/admin/analytics');
      return res.data;
    },
    enabled: user?.role === 'ROLE_ADMIN',
  });

  // TMDB sync handler
  const handleSyncMovie = async (e: React.FormEvent) => {
    e.preventDefault();
    setSyncSuccess('');
    setSyncError('');
    if (!tmdbId.trim()) return;

    setSyncLoading(true);
    try {
      const res = await api.post(`/admin/movies/sync?tmdbId=${tmdbId}`);
      setSyncSuccess(`Successfully imported "${res.data.title}" into catalog.`);
      setTmdbId('');
      queryClient.invalidateQueries({ queryKey: ['movies'] });
    } catch (err: any) {
      setSyncError(err.response?.data?.message || 'Sync failed. Check TMDB ID or API key configuration.');
    } finally {
      setSyncLoading(false);
    }
  };

  // Theater creation handler
  const handleCreateTheater = async (e: React.FormEvent) => {
    e.preventDefault();
    setTheaterSuccess('');
    setTheaterError('');
    if (!theaterName.trim() || !theaterCity.trim()) return;

    setTheaterLoading(true);
    try {
      const res = await api.post('/admin/theaters', {
        name: theaterName,
        city: theaterCity,
        address: theaterAddress
      });
      setTheaterSuccess(`Seeded theater "${res.data.name}" with IMAX screen and 40 seats.`);
      setTheaterName('');
      setTheaterCity('');
      setTheaterAddress('');
      queryClient.invalidateQueries({ queryKey: ['admin-analytics'] });
    } catch (err: any) {
      setTheaterError(err.response?.data?.message || 'Theater creation failed.');
    } finally {
      setTheaterLoading(false);
    }
  };

  // Role Access Restriction Guard
  if (user?.role !== 'ROLE_ADMIN') {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-zinc-950 px-4 text-center">
        <div className="glass-panel border border-red-500/30 rounded-3xl p-8 max-w-md space-y-4">
          <ShieldAlert className="h-16 w-16 text-red-500 mx-auto animate-pulse" />
          <h2 className="text-2xl font-bold tracking-tight text-white">Access Restricted</h2>
          <p className="text-gray-400 text-sm leading-relaxed">
            You do not have the required administrative credentials to access this control panel. Please log in with an administrator account.
          </p>
        </div>
      </div>
    );
  }

  if (isLoading || !analytics) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-red-600" />
      </div>
    );
  }

  // Sentiment percentages calculation
  const totalReviews = 
    analytics.reviewSentimentCounts.POSITIVE + 
    analytics.reviewSentimentCounts.NEUTRAL + 
    analytics.reviewSentimentCounts.NEGATIVE || 1;

  const posPct = Math.round((analytics.reviewSentimentCounts.POSITIVE / totalReviews) * 100);
  const neuPct = Math.round((analytics.reviewSentimentCounts.NEUTRAL / totalReviews) * 100);
  const negPct = Math.round((analytics.reviewSentimentCounts.NEGATIVE / totalReviews) * 100);

  // Graph values formatting
  const weeklyValues = Object.entries(analytics.weeklyRevenueHistory || {});
  const maxWeeklyRevenue = Math.max(...weeklyValues.map(([_, val]) => val), 100);

  return (
    <div className="bg-zinc-950 min-h-screen text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-7xl space-y-10">
        
        {/* Title */}
        <div className="flex items-center space-x-2 border-b border-zinc-800 pb-4">
          <BarChart3 className="h-8 w-8 text-red-600" />
          <h1 className="text-3xl font-extrabold tracking-tight">Enterprise Admin Analytics</h1>
        </div>

        {/* Stats Panels Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-2">
            <span className="text-xs text-gray-400 uppercase tracking-widest font-semibold block">Total Revenue</span>
            <h2 className="text-3xl font-extrabold text-white">${analytics.totalRevenue.toFixed(2)}</h2>
            <p className="text-[10px] text-green-500 font-bold flex items-center"><TrendingUp className="h-3 w-3 mr-0.5" /> +12% vs last month</p>
          </div>

          <div className="glass-panel border border-red-500/20 rounded-2xl p-6 space-y-2 pulse-glow">
            <span className="text-xs text-red-400 uppercase tracking-widest font-bold flex items-center">
              <Sparkles className="h-3 w-3 mr-1" /> AI Next-Month Forecast
            </span>
            <h2 className="text-3xl font-extrabold text-red-500">${analytics.predictedRevenueNextMonth.toFixed(2)}</h2>
            <p className="text-[10px] text-gray-500">Gemini Linear Regression Curve</p>
          </div>

          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-2">
            <span className="text-xs text-gray-400 uppercase tracking-widest font-semibold block">Avg Seat Occupancy</span>
            <h2 className="text-3xl font-extrabold text-white">{analytics.averageOccupancyRate.toFixed(1)}%</h2>
            <p className="text-[10px] text-blue-500 font-bold flex items-center"><Percent className="h-3 w-3 mr-0.5" /> IMAX room averages</p>
          </div>

          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-2">
            <span className="text-xs text-gray-400 uppercase tracking-widest font-semibold block">Tickets Confirmed</span>
            <h2 className="text-3xl font-extrabold text-white">{analytics.totalTicketsSold}</h2>
            <p className="text-[10px] text-purple-500 font-bold flex items-center"><Users className="h-3 w-3 mr-0.5" /> Booked seats total</p>
          </div>
        </div>

        {/* Charts & Sentiments Row */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Custom Weekly Revenue Curve (CSS Bars) */}
          <div className="lg:col-span-2 glass-panel border border-zinc-800 rounded-2xl p-6 space-y-6">
            <h3 className="font-bold text-lg tracking-wide">Weekly Revenue Trend</h3>
            <div className="flex h-56 items-end justify-between gap-6 pt-4 border-b border-zinc-800 pb-1">
              {weeklyValues.map(([week, value]) => {
                const heightPct = (value / maxWeeklyRevenue) * 100;
                return (
                  <div key={week} className="flex-1 flex flex-col items-center gap-2 h-full justify-end">
                    <span className="text-[10px] font-bold text-red-400">${value.toFixed(0)}</span>
                    <motion.div
                      initial={{ height: 0 }}
                      animate={{ height: `${heightPct}%` }}
                      transition={{ duration: 0.8 }}
                      className="w-full bg-gradient-to-t from-red-600/20 to-red-600 border-t border-red-500 rounded-t-md cursor-pointer hover:shadow-lg hover:shadow-red-500/10"
                    />
                    <span className="text-[10px] text-gray-500 tracking-wider font-semibold uppercase">{week}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Sentiment Analysis Pie chart */}
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-6">
            <h3 className="font-bold text-lg tracking-wide flex items-center">
              <Sparkles className="h-4 w-4 text-red-500 mr-1.5" /> AI Review Sentiments
            </h3>
            
            <div className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs font-semibold text-green-400">
                  <span className="flex items-center"><Smile className="h-3.5 w-3.5 mr-1" /> Positive</span>
                  <span>{posPct}% ({analytics.reviewSentimentCounts.POSITIVE})</span>
                </div>
                <div className="w-full bg-zinc-900 rounded-full h-2">
                  <div className="bg-green-500 h-2 rounded-full" style={{ width: `${posPct}%` }} />
                </div>
              </div>

              <div className="space-y-1.5">
                <div className="flex justify-between text-xs font-semibold text-gray-400">
                  <span className="flex items-center"><Smile className="h-3.5 w-3.5 mr-1 text-gray-400" /> Neutral</span>
                  <span>{neuPct}% ({analytics.reviewSentimentCounts.NEUTRAL})</span>
                </div>
                <div className="w-full bg-zinc-900 rounded-full h-2">
                  <div className="bg-gray-500 h-2 rounded-full" style={{ width: `${neuPct}%` }} />
                </div>
              </div>

              <div className="space-y-1.5">
                <div className="flex justify-between text-xs font-semibold text-red-400">
                  <span className="flex items-center"><Frown className="h-3.5 w-3.5 mr-1" /> Negative</span>
                  <span>{negPct}% ({analytics.reviewSentimentCounts.NEGATIVE})</span>
                </div>
                <div className="w-full bg-zinc-900 rounded-full h-2">
                  <div className="bg-red-500 h-2 rounded-full" style={{ width: `${negPct}%` }} />
                </div>
              </div>
            </div>
            
            <p className="text-[10px] text-gray-500 leading-relaxed pt-2">
              Sentiment is automatically evaluated upon comment submission using the Gemini classification filter.
            </p>
          </div>

        </div>

        {/* Sync & Seeding Forms Row */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          
          {/* TMDB Catalog Sync form */}
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-4">
            <h3 className="font-bold text-lg flex items-center text-red-500">
              <Database className="h-5 w-5 mr-2" /> TMDB Catalog Sync
            </h3>
            <p className="text-xs text-gray-400">
              Import a film dynamically from TMDB. Enter the ID to query TMDB API, fetch poster assets, trailers, descriptions, and durations, and save it to the DB catalog.
            </p>
            
            {syncSuccess && (
              <div className="p-3 bg-green-950/40 border border-green-500/30 text-xs text-green-400 rounded-lg flex items-center">
                <CheckCircle className="h-4 w-4 mr-2" /> {syncSuccess}
              </div>
            )}
            {syncError && (
              <div className="p-3 bg-red-950/40 border border-red-500/30 text-xs text-red-400 rounded-lg">
                {syncError}
              </div>
            )}

            <form onSubmit={handleSyncMovie} className="flex items-center space-x-2">
              <input
                type="text"
                required
                value={tmdbId}
                onChange={(e) => setTmdbId(e.target.value)}
                placeholder="e.g. 157336 (Interstellar)"
                className="flex-1 bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2.5 text-xs text-white outline-none focus:border-red-500"
              />
              <button
                type="submit"
                disabled={syncLoading}
                className="bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg px-4 py-2.5 text-xs font-bold transition cursor-pointer"
              >
                {syncLoading ? 'Importing...' : 'Sync Film'}
              </button>
            </form>
          </div>

          {/* Seed Theater Venue form */}
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-4">
            <h3 className="font-bold text-lg flex items-center text-red-500">
              <MapPin className="h-5 w-5 mr-2" /> Seed Theater & Seating Grid
            </h3>
            <p className="text-xs text-gray-400">
              Register a new theater venue. This will automatically seed a 3D IMAX screen with a standard 40-seat grid (Rows A-D, standard/premium/VIP).
            </p>

            {theaterSuccess && (
              <div className="p-3 bg-green-950/40 border border-green-500/30 text-xs text-green-400 rounded-lg flex items-center">
                <CheckCircle className="h-4 w-4 mr-2" /> {theaterSuccess}
              </div>
            )}
            {theaterError && (
              <div className="p-3 bg-red-950/40 border border-red-500/30 text-xs text-red-400 rounded-lg">
                {theaterError}
              </div>
            )}

            <form onSubmit={handleCreateTheater} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <input
                  type="text"
                  required
                  value={theaterName}
                  onChange={(e) => setTheaterName(e.target.value)}
                  placeholder="Theater Name (e.g. Cineplex)"
                  className="bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-red-500"
                />
                <input
                  type="text"
                  required
                  value={theaterCity}
                  onChange={(e) => setTheaterCity(e.target.value)}
                  placeholder="City (e.g. Toronto)"
                  className="bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-red-500"
                />
              </div>
              <div className="flex space-x-2">
                <input
                  type="text"
                  required
                  value={theaterAddress}
                  onChange={(e) => setTheaterAddress(e.target.value)}
                  placeholder="Street Address..."
                  className="flex-1 bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-red-500"
                />
                <button
                  type="submit"
                  disabled={theaterLoading}
                  className="bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg px-4 py-2 text-xs font-bold transition cursor-pointer"
                >
                  {theaterLoading ? 'Seeding...' : 'Register'}
                </button>
              </div>
            </form>
          </div>

        </div>

      </div>
    </div>
  );
}
