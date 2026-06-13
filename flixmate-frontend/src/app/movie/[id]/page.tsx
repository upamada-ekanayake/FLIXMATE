'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '../../../store/authStore';
import api from '../../../services/api';
import { Star, Clock, Calendar, Ticket, ChevronRight, MessageSquare, ThumbsUp, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';

interface Movie {
  id: string;
  title: string;
  description: string;
  genre: string;
  durationMinutes: number;
  posterUrl: string;
  trailerUrl: string;
  averageRating: number;
  releaseDate: string;
  tmdbId: string;
}

interface Showtime {
  id: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  currentPrice: number;
}

interface Review {
  id: string;
  rating: number;
  comment: string;
  sentiment: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
  createdAt: string;
  user?: {
    firstName: string;
    lastName: string;
  };
}

export default function MovieDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const router = useRouter();
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [reviewError, setReviewError] = useState('');

  // 1. Fetch Movie details
  const { data: movie, isLoading: movieLoading } = useQuery<Movie>({
    queryKey: ['movie', id],
    queryFn: async () => {
      const res = await api.get(`/movies/${id}`);
      return res.data;
    },
  });

  // 2. Fetch Movie Showtimes
  const { data: showtimes = [] } = useQuery<Showtime[]>({
    queryKey: ['showtimes', id],
    queryFn: async () => {
      const res = await api.get(`/showtimes/movie/${id}`);
      return res.data;
    },
  });

  // 3. Fetch Movie Reviews
  const { data: reviews = [] } = useQuery<Review[]>({
    queryKey: ['reviews', id],
    queryFn: async () => {
      const res = await api.get(`/reviews/movie/${id}`);
      return res.data;
    },
  });

  // 4. Submit Review mutation
  const submitReviewMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post(`/reviews/movie/${id}?rating=${rating}&comment=${encodeURIComponent(comment)}`);
      return res.data;
    },
    onSuccess: () => {
      setComment('');
      queryClient.invalidateQueries({ queryKey: ['reviews', id] });
      queryClient.invalidateQueries({ queryKey: ['movie', id] });
    },
    onError: (err: any) => {
      setReviewError(err.response?.data?.message || 'Failed to submit review.');
    },
  });

  const handleReviewSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setReviewError('');
    if (!comment.trim()) return;
    submitReviewMutation.mutate();
  };

  if (movieLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-red-600" />
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-white">
        Movie not found.
      </div>
    );
  }

  return (
    <div className="bg-zinc-950 min-h-screen text-white pb-16">
      {/* Backdrop Hero Details */}
      <div className="relative h-[45vh] w-full overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/60 to-transparent z-10" />
        <img
          src={movie.posterUrl || 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop'}
          alt={movie.title}
          className="h-full w-full object-cover object-center opacity-40 filter blur-sm scale-105"
        />
        <div className="absolute inset-0 flex items-end px-4 sm:px-12 pb-8 z-20">
          <div className="flex flex-col sm:flex-row items-center sm:space-x-8 max-w-5xl">
            <img
              src={movie.posterUrl || 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&auto=format&fit=crop'}
              alt={movie.title}
              className="h-56 w-40 rounded-lg shadow-2xl object-cover border border-zinc-800 shrink-0"
            />
            <div className="mt-4 sm:mt-0 text-center sm:text-left space-y-3">
              <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight">{movie.title}</h1>
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 text-xs sm:text-sm text-gray-300">
                <span className="flex items-center text-yellow-500 font-semibold">
                  <Star className="h-4 w-4 fill-yellow-500 mr-1" />
                  {movie.averageRating} Rating
                </span>
                <span className="flex items-center">
                  <Clock className="h-4 w-4 mr-1 text-red-500" />
                  {movie.durationMinutes} minutes
                </span>
                <span className="flex items-center">
                  <Calendar className="h-4 w-4 mr-1 text-blue-500" />
                  {movie.releaseDate}
                </span>
                <span className="px-2 py-0.5 bg-zinc-800 rounded border border-zinc-700 text-xs uppercase font-semibold">
                  {movie.genre}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 mt-12 grid grid-cols-1 lg:grid-cols-3 gap-12">
        {/* Left column: Overview & Showtime Picker (Col-span 2) */}
        <div className="lg:col-span-2 space-y-10">
          <div className="space-y-4">
            <h2 className="text-2xl font-bold tracking-wide border-b border-zinc-800 pb-2">Overview</h2>
            <p className="text-gray-300 leading-relaxed">{movie.description}</p>
          </div>

          {/* Showtime Booking Picker */}
          <div className="space-y-4">
            <h2 className="text-2xl font-bold tracking-wide border-b border-zinc-800 pb-2 flex items-center">
              <Ticket className="h-5 w-5 text-red-500 mr-2" /> Book Showtimes
            </h2>
            {showtimes.length === 0 ? (
              <p className="text-gray-500 text-sm">No showtimes are currently scheduled for this movie.</p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {showtimes.map((st) => (
                  <motion.div
                    key={st.id}
                    className="glass-panel border border-zinc-800 rounded-xl p-4 flex items-center justify-between cursor-pointer hover:border-red-500/50 transition-colors"
                    onClick={() => {
                      if (!user) {
                        router.push('/login');
                      } else {
                        router.push(`/book/${st.id}`);
                      }
                    }}
                    whileHover={{ scale: 1.01 }}
                  >
                    <div className="space-y-1">
                      <p className="font-bold text-sm tracking-wide text-white">
                        {new Date(st.startTime).toLocaleString('en-US', {
                          weekday: 'short',
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </p>
                      <p className="text-xs text-gray-400">Screen IMAX 3D</p>
                    </div>
                    <div className="flex items-center space-x-3 text-right">
                      <div>
                        <span className="text-xs text-red-500 block font-semibold uppercase tracking-wider">AI Price</span>
                        <span className="text-base font-extrabold text-white">${st.currentPrice.toFixed(2)}</span>
                      </div>
                      <ChevronRight className="h-5 w-5 text-gray-500" />
                    </div>
                  </motion.div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right column: Reviews & Sentiments (Col-span 1) */}
        <div className="space-y-8">
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-6">
            <h3 className="font-bold text-lg tracking-wide flex items-center">
              <MessageSquare className="h-5 w-5 text-red-500 mr-2" /> Audience Reviews
            </h3>

            {/* Leave a review form */}
            {user ? (
              <form onSubmit={handleReviewSubmit} className="space-y-4">
                <div className="flex items-center space-x-2">
                  <span className="text-xs text-gray-400 font-semibold">Your Rating:</span>
                  <div className="flex items-center space-x-1">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button
                        key={n}
                        type="button"
                        onClick={() => setRating(n)}
                        className="p-0.5 text-yellow-500 hover:scale-110 transition"
                      >
                        <Star className={`h-5 w-5 ${rating >= n ? 'fill-yellow-500' : ''}`} />
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-2">
                  <textarea
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    required
                    placeholder="Write your review... Comments are automatically classified by AI sentiment analysis."
                    className="w-full h-20 bg-zinc-900 border border-zinc-800 rounded-lg p-2.5 text-xs text-white placeholder-gray-500 outline-none transition focus:border-red-500 focus:ring-1 focus:ring-red-500"
                  />
                  {reviewError && <p className="text-[11px] text-red-400">{reviewError}</p>}
                  <button
                    type="submit"
                    disabled={submitReviewMutation.isPending}
                    className="w-full bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg py-2 text-xs font-bold transition flex items-center justify-center cursor-pointer shadow-md hover:shadow-red-600/10"
                  >
                    {submitReviewMutation.isPending ? 'Analyzing...' : 'Post Review'}
                  </button>
                </div>
              </form>
            ) : (
              <p className="text-xs text-gray-500">
                Please{' '}
                <Link href="/login" className="text-red-500 font-semibold hover:underline">
                  sign in
                </Link>{' '}
                to leave a review.
              </p>
            )}

            {/* Reviews log */}
            <div className="space-y-4 max-h-[350px] overflow-y-auto pr-1">
              {reviews.length === 0 ? (
                <p className="text-xs text-gray-500">No reviews yet. Be the first to share your thoughts!</p>
              ) : (
                reviews.map((r) => (
                  <div key={r.id} className="border-b border-zinc-800 pb-3 space-y-2 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-gray-300">
                        {r.user ? `${r.user.firstName} ${r.user.lastName.substring(0, 1)}.` : 'FlixMate User'}
                      </span>
                      {/* Sentiment Badge */}
                      <span
                        className={`px-2 py-0.5 rounded text-[9px] font-extrabold tracking-wide uppercase border flex items-center ${
                          r.sentiment === 'POSITIVE'
                            ? 'bg-green-950/40 text-green-400 border-green-500/30'
                            : r.sentiment === 'NEGATIVE'
                            ? 'bg-red-950/40 text-red-400 border-red-500/30'
                            : 'bg-zinc-800 text-gray-400 border-zinc-700'
                        }`}
                      >
                        <Sparkles className="h-2.5 w-2.5 mr-0.5" />
                        {r.sentiment || 'NEUTRAL'}
                      </span>
                    </div>
                    <div className="flex items-center space-x-1 text-yellow-500">
                      {Array.from({ length: r.rating }).map((_, i) => (
                        <Star key={i} className="h-3 w-3 fill-yellow-500" />
                      ))}
                    </div>
                    <p className="text-gray-400 leading-relaxed italic">"{r.comment}"</p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
