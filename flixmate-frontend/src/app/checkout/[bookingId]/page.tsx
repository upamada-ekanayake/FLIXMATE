'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import api from '../../../services/api';
import { Timer, AlertTriangle, CreditCard, ShieldCheck } from 'lucide-react';
import { motion } from 'framer-motion';

interface BookingDetail {
  bookingId: string;
  showtimeId: string;
  movieTitle: string;
  theaterName: string;
  screenName: string;
  startTime: string;
  totalPrice: number;
  status: string;
  seatLabels: string[];
  paymentIntentId: string;
}

export default function CheckoutPage() {
  const params = useParams();
  const bookingId = params.bookingId as string;
  const router = useRouter();
  
  const [timeLeft, setTimeLeft] = useState(600); // 10 minutes (600s) default
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError, setPaymentError] = useState('');
  
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');

  // 1. Fetch Booking summary details (temporarily mock or query history)
  const { data: booking, isLoading } = useQuery<BookingDetail>({
    queryKey: ['booking-summary', bookingId],
    queryFn: async () => {
      // In our design, history contains recent bookings
      const res = await api.get('/bookings/history');
      const found = res.data.find((b: any) => b.id === bookingId);
      if (!found) throw new Error('Booking not found');
      
      // Map back to DTO
      return {
        bookingId: found.id,
        showtimeId: found.showtime.id,
        movieTitle: found.showtime.movie.title,
        theaterName: found.showtime.screen.theater.name,
        screenName: found.showtime.screen.name,
        startTime: found.showtime.startTime,
        totalPrice: found.totalPrice,
        status: found.status,
        seatLabels: [], // Seats are mapped inside BookedSeat, let's keep list basic
        paymentIntentId: found.paymentIntentId
      };
    },
    retry: 2
  });

  // Countdown timer logic
  useEffect(() => {
    if (timeLeft <= 0) {
      router.push('/'); // Redirect on expiry
      return;
    }

    const timer = setInterval(() => {
      setTimeLeft((prev) => prev - 1);
    }, 1000);

    return () => clearInterval(timer);
  }, [timeLeft, router]);

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    setPaymentError('');
    setPaymentLoading(true);

    try {
      // Confirm booking transaction via backend API
      const res = await api.post(`/bookings/${bookingId}/confirm`);
      router.push(`/ticket/${res.data.bookingId}`);
    } catch (err: any) {
      setPaymentError(err.response?.data?.message || 'Payment processing failed. Try again.');
    } finally {
      setPaymentLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-red-600" />
      </div>
    );
  }

  return (
    <div className="bg-zinc-950 min-h-screen text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-4xl grid grid-cols-1 md:grid-cols-2 gap-8">
        
        {/* Left Side: Summary & Countdown */}
        <div className="space-y-6">
          <div className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-4">
            <h2 className="text-xl font-bold tracking-wide">Booking Summary</h2>
            {booking && (
              <div className="space-y-2 text-sm text-gray-300">
                <p><span className="text-gray-500">Movie:</span> <strong className="text-white">{booking.movieTitle}</strong></p>
                <p><span className="text-gray-500">Cinema:</span> {booking.theaterName} • {booking.screenName}</p>
                <p><span className="text-gray-500">Showtime:</span> {new Date(booking.startTime).toLocaleString()}</p>
                <p><span className="text-gray-500">Total Bill:</span> <strong className="text-red-500 text-lg">${booking.totalPrice.toFixed(2)}</strong></p>
              </div>
            )}
          </div>

          {/* Seat Hold Release Timer warning */}
          <div className="glass-panel border border-amber-500/30 bg-amber-950/20 rounded-2xl p-6 flex items-start space-x-4">
            <Timer className="h-8 w-8 text-amber-500 shrink-0 animate-pulse" />
            <div className="space-y-1">
              <h4 className="font-bold text-amber-500 text-sm">Temporary Seat Hold Active</h4>
              <p className="text-xs text-gray-300">
                We are holding your seats for <strong className="text-amber-500">{formatTime(timeLeft)}</strong>. Please complete payment before the timer runs out, or these seats will be released to others.
              </p>
            </div>
          </div>
        </div>

        {/* Right Side: Simulated Credit Card Form */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          className="glass-panel border border-zinc-800 rounded-2xl p-6 space-y-6"
        >
          <div className="flex items-center space-x-2 border-b border-zinc-800 pb-3">
            <CreditCard className="h-5 w-5 text-red-500" />
            <h3 className="font-bold text-lg">Checkout Payment</h3>
          </div>

          {paymentError && (
            <div className="rounded-lg bg-red-900/30 border border-red-500/50 p-4 text-xs text-red-400 flex items-center">
              <AlertTriangle className="h-5 w-5 mr-2 shrink-0" />
              <span>{paymentError}</span>
            </div>
          )}

          <form onSubmit={handlePay} className="space-y-4 text-sm">
            <div className="space-y-1">
              <label className="text-xs text-gray-400 font-semibold uppercase">Cardholder Name</label>
              <input
                type="text"
                required
                placeholder="John Doe"
                className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-white outline-none focus:border-red-500"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs text-gray-400 font-semibold uppercase">Card Number</label>
              <input
                type="text"
                required
                maxLength={16}
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value.replace(/\D/g, ''))}
                placeholder="4111 2222 3333 4444"
                className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-white outline-none focus:border-red-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-xs text-gray-400 font-semibold uppercase">Expiry (MM/YY)</label>
                <input
                  type="text"
                  required
                  maxLength={5}
                  value={cardExpiry}
                  onChange={(e) => setCardExpiry(e.target.value)}
                  placeholder="12/28"
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-white outline-none focus:border-red-500"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs text-gray-400 font-semibold uppercase">CVV</label>
                <input
                  type="password"
                  required
                  maxLength={3}
                  value={cardCvv}
                  onChange={(e) => setCardCvv(e.target.value.replace(/\D/g, ''))}
                  placeholder="321"
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-white outline-none focus:border-red-500"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={paymentLoading}
              className="w-full py-3.5 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold rounded-lg transition-all flex items-center justify-center cursor-pointer shadow-lg shadow-red-600/20"
            >
              {paymentLoading ? 'Authorizing Checkout...' : `Confirm Payment`}
            </button>

            <div className="flex items-center justify-center space-x-2 text-[10px] text-gray-500 uppercase tracking-widest pt-2">
              <ShieldCheck className="h-4 w-4 text-green-500" />
              <span>Simulated Sandbox Secure Payment</span>
            </div>
          </form>
        </motion.div>
        
      </div>
    </div>
  );
}
