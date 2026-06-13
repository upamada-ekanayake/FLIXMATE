import type { Metadata } from 'next';
import './globals.css';
import Providers from '../components/Providers';
import Navbar from '../components/Navbar';
import AIChatbot from '../components/AIChatbot';

export const metadata: Metadata = {
  title: 'FlixMate | Premium Movie Ticket Booking Platform',
  description: 'Book movie tickets instantly with real-time seat locks, dynamic pricing, and AI-driven personalized recommendations on FlixMate.',
  keywords: ['Movie Ticket Booking', 'Cinema Tickets', 'Real-time Seat Selection', 'IMAX', 'FlixMate'],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased selection:bg-red-500/30 selection:text-red-500">
        <Providers>
          <Navbar />
          <main className="min-h-[calc(100vh-4rem)]">
            {children}
          </main>
          <AIChatbot />
        </Providers>
      </body>
    </html>
  );
}
