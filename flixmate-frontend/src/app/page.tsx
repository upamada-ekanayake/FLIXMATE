'use client';

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import Link from 'next/link';
import { Search, Star, Film, Sparkles, Send, Play, Clock, ArrowRight } from 'lucide-react';
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

export default function HomePage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedGenre, setSelectedGenre] = useState('All');
  const [aiPreferences, setAiPreferences] = useState('');
  const [aiRecommendation, setAiRecommendation] = useState('');
  const [aiLoading, setAiLoading] = useState(false);

  // Fetch Movies Catalog
  const { data: movies = [], isLoading } = useQuery<Movie[]>({
    queryKey: ['movies'],
    queryFn: async () => {
      const res = await api.get('/movies');
      return res.data;
    },
  });

  // Query Gemini Recommendations
  const handleAskAI = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!aiPreferences.trim()) return;

    setAiLoading(true);
    setAiRecommendation('');
    try {
      const res = await api.get(`/movies/recommend?preferences=${encodeURIComponent(aiPreferences)}`);
      setAiRecommendation(res.data);
    } catch (err) {
      setAiRecommendation('Failed to generate recommendations. Please try again later.');
    } finally {
      setAiLoading(false);
    }
  };

  const filteredMovies = movies.filter((m) => {
    const matchesSearch = m.title.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          m.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesGenre = selectedGenre === 'All' || m.genre.includes(selectedGenre);
    return matchesSearch && matchesGenre;
  });

  const genres = ['All', 'Action', 'Sci-Fi', 'Adventure', 'Drama', 'Thriller'];

  // Hero Movie (Use the first movie, Interstellar, if available)
  const heroMovie = movies[0];

  return (
    <div className="bg-zinc-950 min-h-screen text-white pb-16">
      {/* Hero Banner (IMAX / Netflix style) */}
      {heroMovie && (
        <div className="relative h-[65vh] w-full overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/40 to-transparent z-10" />
          <div className="absolute inset-0 bg-gradient-to-r from-zinc-950 via-zinc-950/20 to-transparent z-10" />
          <img
            src={heroMovie.posterUrl || 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1600&auto=format&fit=crop'}
            alt={heroMovie.title}
            className="h-full w-full object-cover object-top opacity-50 scale-105 transition-transform duration-10000"
          />

          <div className="absolute bottom-12 left-4 sm:left-12 z-20 max-w-2xl space-y-4">
            <span className="px-3 py-1 rounded-full bg-red-600/30 text-red-500 border border-red-500/50 text-xs font-bold uppercase tracking-wider">
              Featured Blockbuster
            </span>
            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight drop-shadow-lg">
              {heroMovie.title}
            </h1>
            <div className="flex items-center space-x-4 text-sm text-gray-300">
              <span className="flex items-center text-yellow-500 font-semibold">
                <Star className="h-4 w-4 fill-yellow-500 mr-1" />
                {heroMovie.averageRating} Rating
              </span>
              <span className="flex items-center">
                <Clock className="h-4 w-4 mr-1 text-red-500" />
                {heroMovie.durationMinutes} min
              </span>
              <span className="px-2 py-0.5 bg-zinc-800 rounded border border-zinc-700 text-xs">
                {heroMovie.genre}
              </span>
            </div>
            <p className="text-gray-300 text-sm sm:text-base leading-relaxed line-clamp-3">
              {heroMovie.description}
            </p>
            <div className="flex items-center space-x-4 pt-2">
              <Link
                href={`/movie/${heroMovie.id}`}
                className="px-6 py-3 bg-red-600 hover:bg-red-700 text-white rounded-lg transition font-bold flex items-center shadow-lg shadow-red-600/20"
              >
                <Play className="h-4 w-4 fill-white mr-2" />
                Book Tickets
              </Link>
              {heroMovie.trailerUrl && (
                <a
                  href={heroMovie.trailerUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-6 py-3 bg-zinc-800 hover:bg-zinc-700 text-white rounded-lg transition font-semibold border border-zinc-700"
                >
                  Watch Trailer
                </a>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Main Container */}
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 mt-12 space-y-12">
        {/* Search & AI Section Row */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          {/* Movie Catalog Filters (Col-span 2) */}
          <div className="lg:col-span-2 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <h2 className="text-2xl font-bold tracking-wide flex items-center">
                <Film className="h-6 w-6 text-red-500 mr-2" /> Explore Movies
              </h2>
              {/* Search Box */}
              <div className="relative max-w-xs w-full">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-4 w-4 text-gray-500" />
                </div>
                <input
                  type="text"
                  placeholder="Search movies..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="block w-full bg-zinc-900 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500"
                />
              </div>
            </div>

            {/* Genre Pills */}
            <div className="flex items-center space-x-2 overflow-x-auto pb-2">
              {genres.map((genre) => (
                <button
                  key={genre}
                  onClick={() => setSelectedGenre(genre)}
                  className={`px-4 py-1.5 rounded-full text-xs font-semibold tracking-wide border transition cursor-pointer shrink-0 ${
                    selectedGenre === genre
                      ? 'bg-red-600 text-white border-red-500 shadow-md shadow-red-600/10'
                      : 'bg-zinc-900 text-gray-400 border-zinc-800 hover:text-white hover:border-zinc-700'
                  }`}
                >
                  {genre}
                </button>
              ))}
            </div>

            {/* Movies Grid */}
            {isLoading ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-6">
                {[1, 2, 3].map((n) => (
                  <div key={n} className="h-80 bg-zinc-900 animate-pulse rounded-xl" />
                ))}
              </div>
            ) : filteredMovies.length === 0 ? (
              <div className="text-center py-12 text-gray-500 font-medium">
                No movies found matching your criteria.
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-6">
                {filteredMovies.map((movie) => (
                  <motion.div
                    key={movie.id}
                    className="glass-panel overflow-hidden rounded-xl border border-zinc-900 movie-card-glow cursor-pointer"
                    whileHover={{ scale: 1.02 }}
                  >
                    <Link href={`/movie/${movie.id}`}>
                      <div className="relative aspect-[3/4] overflow-hidden bg-zinc-900">
                        <img
                          src={movie.posterUrl || 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop'}
                          alt={movie.title}
                          className="h-full w-full object-cover transition-transform duration-500 hover:scale-105"
                        />
                        <div className="absolute top-2 right-2 bg-black/70 backdrop-blur-md px-2 py-0.5 rounded text-[10px] font-bold text-yellow-500 flex items-center">
                          <Star className="h-3 w-3 fill-yellow-500 mr-0.5" />
                          {movie.averageRating}
                        </div>
                      </div>
                      <div className="p-4 space-y-2">
                        <h3 className="font-bold text-sm sm:text-base tracking-wide truncate">{movie.title}</h3>
                        <div className="flex items-center justify-between text-[11px] text-gray-400">
                          <span>{movie.genre}</span>
                          <span className="flex items-center text-red-500 font-medium">
                            Book Now <ArrowRight className="h-3 w-3 ml-0.5" />
                          </span>
                        </div>
                      </div>
                    </Link>
                  </motion.div>
                ))}
              </div>
            )}
          </div>

          {/* AI Recommendation Engine Sidepanel (Col-span 1) */}
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-6">
            <div className="flex items-center space-x-2 text-red-500">
              <Sparkles className="h-5 w-5 animate-pulse" />
              <h3 className="font-bold tracking-wide text-lg text-white">AI Suggestion Box</h3>
            </div>
            <p className="text-xs text-gray-400 leading-relaxed">
              Tell FlixMate what kind of experience you want (e.g. "mind bending sci-fi like interstellar" or "fast action and gladiators") and let Gemini suggest matches.
            </p>

            <form onSubmit={handleAskAI} className="space-y-3">
              <textarea
                value={aiPreferences}
                onChange={(e) => setAiPreferences(e.target.value)}
                placeholder="e.g. A deep, thought-provoking film with beautiful visuals and epic spaceships..."
                className="w-full h-24 bg-zinc-900 hover:bg-zinc-850 focus:bg-zinc-850 text-xs text-white placeholder-gray-500 border border-zinc-800 rounded-lg p-3 outline-none transition focus:border-red-500 focus:ring-1 focus:ring-red-500"
              />
              <button
                type="submit"
                disabled={aiLoading}
                className="w-full bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg py-2.5 text-xs font-bold flex items-center justify-center transition shadow-md hover:shadow-red-600/10 cursor-pointer"
              >
                {aiLoading ? 'Asking Gemini...' : 'Analyze & Recommend'}
                {!aiLoading && <Send className="h-3 w-3 ml-1.5" />}
              </button>
            </form>

            {/* AI Recommendation Response Display */}
            {aiRecommendation && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-zinc-900/50 border border-zinc-800 rounded-xl p-4 text-xs leading-relaxed text-gray-300 whitespace-pre-line"
              >
                <div className="font-bold text-white mb-2 flex items-center">
                  <Sparkles className="h-4 w-4 text-red-500 mr-1.5 shrink-0" />
                  Gemini Suggestions:
                </div>
                {aiRecommendation}
              </motion.div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
