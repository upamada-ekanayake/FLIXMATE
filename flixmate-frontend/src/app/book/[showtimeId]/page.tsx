'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import api from '../../../services/api';
import { Armchair, ChevronLeft, CreditCard, Sparkles, AlertCircle } from 'lucide-react';
import { motion } from 'framer-motion';

interface SeatStatus {
  seatId: string;
  rowName: string;
  seatNumber: number;
  type: 'STANDARD' | 'PREMIUM' | 'VIP';
  status: 'AVAILABLE' | 'HOLD' | 'BOOKED';
}

interface Showtime {
  id: string;
  movie: {
    title: string;
  };
  screen: {
    name: string;
    theater: {
      name: string;
    };
  };
  startTime: string;
  currentPrice: number;
}

export default function SeatBookingPage() {
  const params = useParams();
  const showtimeId = params.showtimeId as string;
  const router = useRouter();
  
  const [seats, setSeats] = useState<SeatStatus[]>([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState<string[]>([]);
  const [bookingError, setBookingError] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);

  // 1. Fetch Showtime info
  const { data: showtime } = useQuery<Showtime>({
    queryKey: ['showtime', showtimeId],
    queryFn: async () => {
      const res = await api.get(`/showtimes/${showtimeId}`);
      return res.data;
    },
  });

  // 2. Fetch Initial Seat status
  useEffect(() => {
    const fetchSeats = async () => {
      try {
        const res = await api.get(`/showtimes/${showtimeId}/seats`);
        setSeats(res.data);
      } catch (err) {
        console.error('Failed to load seating map.', err);
      }
    };
    fetchSeats();
  }, [showtimeId]);

  // 3. Connect STOMP WebSockets client-side
  useEffect(() => {
    let stompClient: any;

    try {
      // Require libraries client-side to prevent Next.js SSR build errors
      const SockJS = require('sockjs-client');
      const { Client } = require('@stomp/stompjs');

      const socket = new SockJS('http://localhost:8080/ws');
      stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str: string) => {
          // console.log(str);
        },
        onConnect: () => {
          stompClient.subscribe(`/topic/showtime/${showtimeId}`, (message: any) => {
            if (message.body) {
              const updatedSeats = JSON.parse(message.body);
              setSeats(updatedSeats);
            }
          });
        },
        onStompError: (frame: any) => {
          console.error('STOMP broker connection failed', frame);
        }
      });

      stompClient.activate();
    } catch (e) {
      console.error('WebSocket client init failed, polling fallback will take over updates.', e);
    }

    return () => {
      if (stompClient) {
        stompClient.deactivate();
      }
    };
  }, [showtimeId]);

  // Toggle selection
  const handleSeatClick = (seat: SeatStatus) => {
    if (seat.status !== 'AVAILABLE') return;

    setSelectedSeatIds((prev) => {
      if (prev.includes(seat.seatId)) {
        return prev.filter((id) => id !== seat.seatId);
      } else {
        return [...prev, seat.seatId];
      }
    });
  };

  // Calculate prices dynamically
  const calculateTotal = () => {
    if (!showtime) return 0;
    let total = 0;
    selectedSeatIds.forEach((id) => {
      const seat = seats.find((s) => s.seatId === id);
      if (seat) {
        let price = showtime.currentPrice;
        if (seat.type === 'PREMIUM') price *= 1.2;
        if (seat.type === 'VIP') price *= 1.5;
        total += price;
      }
    });
    return total;
  };

  // Call booking hold API
  const handleHoldSeats = async () => {
    if (selectedSeatIds.length === 0) return;
    
    setBookingError('');
    setBookingLoading(true);

    try {
      const res = await api.post('/bookings/hold', {
        showtimeId,
        seatIds: selectedSeatIds
      });
      // Redirect to simulated checkout screen
      router.push(`/checkout/${res.data.bookingId}`);
    } catch (err: any) {
      setBookingError(err.response?.data?.message || 'Error occurred while locking seats.');
    } finally {
      setBookingLoading(false);
    }
  };

  // Group seats by rows for rendering grid
  const rows = Array.from(new Set(seats.map((s) => s.rowName))).sort();

  return (
    <div className="bg-zinc-950 min-h-screen text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-5xl space-y-8">
        
        {/* Header navigation bar */}
        <div className="flex items-center justify-between border-b border-zinc-800 pb-4">
          <button onClick={() => router.back()} className="flex items-center text-sm text-gray-400 hover:text-white transition">
            <ChevronLeft className="h-5 w-5 mr-1" /> Back to Movie
          </button>
          {showtime && (
            <div className="text-right">
              <h1 className="font-extrabold text-lg sm:text-xl tracking-wide">{showtime.movie.title}</h1>
              <p className="text-xs text-gray-400">
                {showtime.screen.theater.name} • {showtime.screen.name}
              </p>
            </div>
          )}
        </div>

        {/* Screen Graphic & Seat Map */}
        <div className="glass-panel border border-zinc-800 rounded-2xl p-6 sm:p-12 flex flex-col items-center">
          
          {/* Cinema Screen Mockup */}
          <div className="w-full max-w-md bg-white h-2 rounded-t-full opacity-60 cinema-screen-glow mb-2" />
          <p className="text-[10px] uppercase font-bold tracking-widest text-zinc-500 mb-12">Cinema Screen</p>

          {/* Seat Grid Map */}
          {seats.length === 0 ? (
            <div className="animate-pulse flex space-x-2 p-12">
              <Armchair className="h-8 w-8 text-zinc-800" />
              <Armchair className="h-8 w-8 text-zinc-800" />
              <Armchair className="h-8 w-8 text-zinc-800" />
            </div>
          ) : (
            <div className="space-y-3 w-full max-w-lg">
              {rows.map((rowName) => {
                const rowSeats = seats
                  .filter((s) => s.rowName === rowName)
                  .sort((a, b) => a.seatNumber - b.seatNumber);

                return (
                  <div key={rowName} className="flex items-center justify-between space-x-4">
                    <span className="text-xs font-bold text-zinc-500 w-4">{rowName}</span>
                    <div className="flex-1 flex justify-center space-x-2 sm:space-x-3">
                      {rowSeats.map((seat) => {
                        const isSelected = selectedSeatIds.includes(seat.seatId);
                        const isOccupied = seat.status !== 'AVAILABLE';

                        // Calculate theme based on seat category
                        let seatColor = 'text-zinc-600 hover:text-zinc-400';
                        if (seat.type === 'PREMIUM') seatColor = 'text-blue-500 hover:text-blue-400';
                        if (seat.type === 'VIP') seatColor = 'text-purple-500 hover:text-purple-400';

                        if (isSelected) seatColor = 'text-red-500 drop-shadow-[0_0_8px_rgba(239,68,68,0.6)] scale-110';
                        if (isOccupied) seatColor = 'text-zinc-850 opacity-40 cursor-not-allowed';

                        return (
                          <button
                            key={seat.seatId}
                            disabled={isOccupied}
                            onClick={() => handleSeatClick(seat)}
                            className="transition-all duration-200 outline-none"
                            title={`Seat ${seat.rowName}-${seat.seatNumber} (${seat.type}) - ${seat.status}`}
                          >
                            <Armchair className={`h-6 w-6 sm:h-7 sm:w-7 ${seatColor}`} />
                          </button>
                        );
                      })}
                    </div>
                    <span className="text-xs font-bold text-zinc-500 w-4 text-right">{rowName}</span>
                  </div>
                );
              })}
            </div>
          )}

          {/* Seat Type Legends */}
          <div className="flex flex-wrap justify-center gap-6 mt-12 text-[10px] uppercase font-bold text-gray-400">
            <span className="flex items-center"><Armchair className="h-4 w-4 text-zinc-600 mr-1.5" /> Standard</span>
            <span className="flex items-center"><Armchair className="h-4 w-4 text-blue-500 mr-1.5" /> Premium (+20%)</span>
            <span className="flex items-center"><Armchair className="h-4 w-4 text-purple-500 mr-1.5" /> VIP (+50%)</span>
            <span className="flex items-center"><Armchair className="h-4 w-4 text-red-500 mr-1.5" /> Selected</span>
            <span className="flex items-center"><Armchair className="h-4 w-4 text-zinc-850 mr-1.5" /> Reserved</span>
          </div>
        </div>

        {/* Error messaging */}
        {bookingError && (
          <div className="rounded-lg bg-red-900/30 border border-red-500/50 p-4 text-sm text-red-400 flex items-center">
            <AlertCircle className="h-5 w-5 mr-2" />
            <span>{bookingError}</span>
          </div>
        )}

        {/* Checkout detail summary pane */}
        <div className="glass-panel border border-zinc-800 rounded-2xl p-6 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="space-y-1 text-center md:text-left">
            <span className="text-xs text-red-500 font-bold uppercase tracking-wider flex items-center justify-center md:justify-start">
              <Sparkles className="h-3 w-3 mr-1" /> Dynamic Booking Cost
            </span>
            <p className="text-gray-400 text-xs">
              Selected seats: {selectedSeatIds.length > 0 
                ? selectedSeatIds.map(id => {
                    const s = seats.find(x => x.seatId === id);
                    return s ? `${s.rowName}-${s.seatNumber}` : '';
                  }).join(', ')
                : 'None'}
            </p>
            <h2 className="text-2xl font-extrabold text-white">Total: ${calculateTotal().toFixed(2)}</h2>
          </div>

          <button
            onClick={handleHoldSeats}
            disabled={selectedSeatIds.length === 0 || bookingLoading}
            className="w-full md:w-auto px-8 py-3.5 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold rounded-lg transition-all flex items-center justify-center cursor-pointer shadow-lg shadow-red-600/20"
          >
            <CreditCard className="h-4 w-4 mr-2" />
            {bookingLoading ? 'Locking seats...' : 'Reserve & Checkout'}
          </button>
        </div>

      </div>
    </div>
  );
}
