'use client';

import React from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import api from '../../../services/api';
import { useAuthStore } from '../../../store/authStore';
import { CheckCircle2, Download, Home, Film, Calendar, MapPin, QrCode } from 'lucide-react';
import { motion } from 'framer-motion';

interface BookingResponse {
  bookingId: string;
  showtimeId: string;
  movieTitle: string;
  theaterName: string;
  screenName: string;
  startTime: string;
  totalPrice: number;
  status: string;
  seatLabels: string[];
  qrCodeBase64: string;
  paymentIntentId: string;
}

export default function TicketConfirmationPage() {
  const params = useParams();
  const bookingId = params.bookingId as string;
  const router = useRouter();
  const { user } = useAuthStore();

  // Fetch Booking Details (confirming and showing QR)
  const { data: booking, isLoading } = useQuery<BookingResponse>({
    queryKey: ['confirmed-ticket', bookingId],
    queryFn: async () => {
      // Direct call confirm to fetch finalized state
      const res = await api.post(`/bookings/${bookingId}/confirm`);
      return res.data;
    },
    retry: false
  });

  const handleDownloadPDF = async () => {
    try {
      const res = await api.get(`/bookings/${bookingId}/pdf`, {
        responseType: 'blob', // Important for file handling
      });
      
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `FlixMate_Ticket_${bookingId.toString().substring(0, 8)}.pdf`;
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

  if (!booking) {
    return (
      <div className="flex h-screen items-center justify-center bg-zinc-950 text-white">
        Booking details unavailable.
      </div>
    );
  }

  return (
    <div className="bg-zinc-950 min-h-screen text-white py-12 px-4 sm:px-6 lg:px-8 flex items-center justify-center">
      <div className="max-w-md w-full space-y-8 text-center">
        
        {/* Success Header Animation */}
        <motion.div
          initial={{ scale: 0.5, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 260, damping: 20 }}
          className="space-y-3 flex flex-col items-center"
        >
          <CheckCircle2 className="h-16 w-16 text-green-500 animate-bounce" />
          <h1 className="text-3xl font-extrabold tracking-tight">Booking Confirmed!</h1>
          <p className="text-sm text-gray-400">
            Enjoy the show! Your receipt and tickets have been generated.
          </p>
        </motion.div>

        {/* E-Ticket layout card */}
        <motion.div
          initial={{ y: 50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="glass-panel border border-zinc-800 rounded-3xl overflow-hidden shadow-2xl relative"
        >
          {/* Cinema ticket punch notches */}
          <div className="absolute top-1/2 -left-3 h-6 w-6 rounded-full bg-zinc-950 border border-zinc-850 z-20" />
          <div className="absolute top-1/2 -right-3 h-6 w-6 rounded-full bg-zinc-950 border border-zinc-850 z-20" />

          {/* Top section */}
          <div className="p-6 space-y-4 text-left border-b border-dashed border-zinc-850">
            <h3 className="font-extrabold text-xl tracking-wide text-white">{booking.movieTitle}</h3>
            
            <div className="space-y-2 text-xs text-gray-300">
              <p className="flex items-center"><MapPin className="h-4 w-4 mr-2 text-red-500" /> {booking.theaterName} • {booking.screenName}</p>
              <p className="flex items-center"><Calendar className="h-4 w-4 mr-2 text-blue-500" /> {new Date(booking.startTime).toLocaleString()}</p>
              <p className="flex items-center"><Film className="h-4 w-4 mr-2 text-purple-500" /> Seats: <strong className="text-white ml-1">{booking.seatLabels.join(', ') || 'A-1'}</strong></p>
            </div>
          </div>

          {/* Bottom section (QR Code) */}
          <div className="p-6 flex flex-col items-center space-y-4">
            {booking.qrCodeBase64 ? (
              <img
                src={`data:image/png;base64,${booking.qrCodeBase64}`}
                alt="Booking Verification QR"
                className="h-40 w-40 rounded-xl bg-white p-2 border border-zinc-800"
              />
            ) : (
              <div className="h-40 w-40 rounded-xl bg-zinc-900 border border-zinc-800 flex items-center justify-center">
                <QrCode className="h-12 w-12 text-zinc-700 animate-pulse" />
              </div>
            )}
            
            <div className="space-y-1">
              <span className="text-[10px] text-gray-500 uppercase tracking-widest block font-bold">Ticket Identifier</span>
              <code className="text-xs text-red-400 font-mono font-semibold">{booking.bookingId.substring(0, 18)}...</code>
            </div>
          </div>
        </motion.div>

        {/* Option Actions buttons */}
        <div className="flex flex-col sm:flex-row gap-4">
          <button
            onClick={handleDownloadPDF}
            className="flex-1 py-3 px-4 bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 font-bold rounded-lg text-sm text-white transition flex items-center justify-center cursor-pointer"
          >
            <Download className="h-4 w-4 mr-2" /> Download PDF
          </button>
          <button
            onClick={() => router.push('/')}
            className="flex-1 py-3 px-4 bg-red-600 hover:bg-red-700 font-bold rounded-lg text-sm text-white transition flex items-center justify-center cursor-pointer shadow-lg shadow-red-600/25"
          >
            <Home className="h-4 w-4 mr-2" /> Back to Home
          </button>
        </div>

      </div>
    </div>
  );
}
