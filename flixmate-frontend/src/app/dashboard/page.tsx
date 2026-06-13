'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Ticket, Calendar, ShieldCheck, Download, AlertCircle, ShoppingBag } from 'lucide-react';
import { motion } from 'framer-motion';

interface Booking {
  id: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  totalPrice: number;
  createdAt: string;
  showtime: {
    startTime: string;
    movie: {
      title: string;
    };
    screen: {
      name: string;
      theater: {
        name: string;
      };
    };
  };
}

export default function UserDashboard() {
  const { user } = useAuthStore();

  // Fetch booking histories
  const { data: bookings = [], isLoading } = useQuery<Booking[]>({
    queryKey: ['booking-history'],
    queryFn: async () => {
      const res = await api.get('/bookings/history');
      return res.data;
    },
    enabled: !!user,
  });

  const handleDownloadPDF = async (bookingId: string) => {
    try {
      const res = await api.get(`/bookings/${bookingId}/pdf`, {
        responseType: 'blob',
      });
      
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `FlixMate_Ticket_${bookingId.substring(0, 8)}.pdf`;
      link.click();
    } catch (e) {
      console.error('Failed to download PDF ticket.', e);
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
      <div className="mx-auto max-w-4xl space-y-8">
        
        {/* User Card */}
        <div className="glass-panel border border-zinc-800 rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center space-x-4">
            <div className="h-16 w-16 rounded-full bg-red-600/20 text-red-500 border border-red-500/30 flex items-center justify-center font-extrabold text-2xl">
              {user?.firstName.substring(0, 1)}{user?.lastName.substring(0, 1)}
            </div>
            <div className="text-center sm:text-left space-y-1">
              <h2 className="text-xl sm:text-2xl font-bold">{user?.firstName} {user?.lastName}</h2>
              <p className="text-xs text-gray-400">{user?.email}</p>
            </div>
          </div>
          <div className="px-4 py-1.5 rounded-full bg-zinc-900 border border-zinc-800 text-xs font-semibold text-red-500 uppercase tracking-widest flex items-center">
            <ShieldCheck className="h-4 w-4 mr-1 text-green-500" /> {user?.role.replace('ROLE_', '')} Account
          </div>
        </div>

        {/* Bookings Logs */}
        <div className="space-y-6">
          <h2 className="text-2xl font-extrabold tracking-wide flex items-center border-b border-zinc-800 pb-2">
            <ShoppingBag className="h-6 w-6 text-red-500 mr-2" /> Booking History
          </h2>

          {bookings.length === 0 ? (
            <div className="text-center py-12 text-gray-500 font-medium">
              You haven't booked any movie tickets yet.
            </div>
          ) : (
            <div className="space-y-4">
              {bookings.map((booking) => (
                <motion.div
                  key={booking.id}
                  className="glass-panel border border-zinc-800 rounded-2xl p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
                  whileHover={{ scale: 1.005 }}
                >
                  <div className="space-y-3">
                    <div className="flex items-center space-x-2.5">
                      <strong className="text-base sm:text-lg text-white font-extrabold tracking-wide">{booking.showtime.movie.title}</strong>
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                          booking.status === 'CONFIRMED'
                            ? 'bg-green-950/40 text-green-400 border border-green-500/20'
                            : booking.status === 'PENDING'
                            ? 'bg-amber-950/40 text-amber-400 border border-amber-500/20'
                            : 'bg-red-950/40 text-red-400 border border-red-500/20'
                        }`}
                      >
                        {booking.status}
                      </span>
                    </div>

                    <div className="space-y-1 text-xs text-gray-300">
                      <p className="flex items-center"><Ticket className="h-4 w-4 mr-1.5 text-red-500" /> {booking.showtime.screen.theater.name} • {booking.showtime.screen.name}</p>
                      <p className="flex items-center"><Calendar className="h-4 w-4 mr-1.5 text-blue-500" /> {new Date(booking.showtime.startTime).toLocaleString()}</p>
                    </div>
                  </div>

                  <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center border-t sm:border-t-0 border-zinc-800 pt-3 sm:pt-0 gap-3">
                    <div className="text-left sm:text-right">
                      <span className="text-[10px] text-gray-500 uppercase block font-semibold">Total Paid</span>
                      <strong className="text-base sm:text-lg text-white">${booking.totalPrice.toFixed(2)}</strong>
                    </div>
                    {booking.status === 'CONFIRMED' ? (
                      <button
                        onClick={() => handleDownloadPDF(booking.id)}
                        className="py-1.5 px-3 bg-zinc-800 hover:bg-zinc-700 rounded-lg text-xs font-bold text-white transition flex items-center cursor-pointer border border-zinc-700"
                      >
                        <Download className="h-3.5 w-3.5 mr-1" /> PDF Ticket
                      </button>
                    ) : (
                      <span className="text-xs text-gray-500 flex items-center">
                        <AlertCircle className="h-3.5 w-3.5 mr-1 text-zinc-500" /> History Locked
                      </span>
                    )}
                  </div>
                </motion.div>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
