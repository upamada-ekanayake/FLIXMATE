'use client';

import React, { useState, useRef, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import api from '../services/api';
import { MessageSquare, X, Send, Bot, User, HelpCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface Message {
  sender: 'bot' | 'user';
  text: string;
}

export default function AIChatbot() {
  const { user } = useAuthStore();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    { sender: 'bot', text: 'Hi! I am your FlixMate Assistant. How can I help you with tickets, refunds, or seat choices today?' }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (textToSend?: string) => {
    const messageText = textToSend || input;
    if (!messageText.trim()) return;

    if (!user) {
      setMessages((prev) => [
        ...prev,
        { sender: 'user', text: messageText },
        { sender: 'bot', text: 'Please log in to chat with our AI Support Assistant.' }
      ]);
      if (!textToSend) setInput('');
      return;
    }

    setMessages((prev) => [...prev, { sender: 'user', text: messageText }]);
    if (!textToSend) setInput('');
    setLoading(true);

    try {
      const res = await api.post('/chatbot/query', { message: messageText });
      setMessages((prev) => [...prev, { sender: 'bot', text: res.data.response }]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        { sender: 'bot', text: 'Sorry, I am having trouble connecting right now. Please try again later.' }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const presets = [
    'How do I cancel a ticket?',
    'What is the 10-minute seat lock?',
    'Show recommendations for action movies'
  ];

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {/* Toggle Bubble */}
      {!isOpen && (
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={() => setIsOpen(true)}
          className="flex h-14 w-14 items-center justify-center rounded-full bg-red-600 hover:bg-red-700 text-white shadow-xl hover:shadow-red-600/30 pulse-glow cursor-pointer"
          aria-label="Open Chatbot"
        >
          <MessageSquare className="h-6 w-6" />
        </motion.button>
      )}

      {/* Chat Window */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className="glass-panel flex h-[500px] w-96 flex-col overflow-hidden rounded-2xl border"
          >
            {/* Header */}
            <div className="flex items-center justify-between bg-red-600 px-4 py-3 text-white">
              <div className="flex items-center space-x-2">
                <Bot className="h-5 w-5" />
                <span className="font-bold tracking-wide">FlixMate AI Concierge</span>
              </div>
              <button onClick={() => setIsOpen(false)} className="hover:opacity-80">
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Message History */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.map((m, idx) => (
                <div
                  key={idx}
                  className={`flex ${m.sender === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`flex max-w-[80%] items-start space-x-2 rounded-xl p-3 text-sm leading-relaxed ${
                      m.sender === 'user'
                        ? 'bg-red-600 text-white rounded-br-none'
                        : 'bg-zinc-800 text-gray-200 rounded-bl-none border border-zinc-700'
                    }`}
                  >
                    {m.sender === 'bot' ? (
                      <Bot className="h-4 w-4 mt-0.5 shrink-0 text-red-400" />
                    ) : (
                      <User className="h-4 w-4 mt-0.5 shrink-0 text-gray-300" />
                    )}
                    <span>{m.text}</span>
                  </div>
                </div>
              ))}
              {loading && (
                <div className="flex justify-start">
                  <div className="bg-zinc-800 text-gray-400 border border-zinc-700 rounded-xl rounded-bl-none p-3 text-sm flex items-center space-x-2">
                    <Bot className="h-4 w-4 text-red-400 animate-pulse" />
                    <span className="animate-pulse">Thinking...</span>
                  </div>
                </div>
              )}
              <div ref={scrollRef} />
            </div>

            {/* Presets */}
            {messages.length === 1 && (
              <div className="px-4 pb-2 space-y-2">
                <p className="text-xs text-gray-500 font-semibold flex items-center">
                  <HelpCircle className="h-3 w-3 mr-1" /> Frequently Asked Questions:
                </p>
                <div className="flex flex-col space-y-1.5">
                  {presets.map((p, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSend(p)}
                      className="text-left text-xs bg-zinc-800 hover:bg-zinc-750 text-gray-300 border border-zinc-700/60 rounded-md py-1.5 px-2.5 transition"
                    >
                      {p}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Input field */}
            <div className="border-t border-zinc-800 bg-zinc-900/40 p-3">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSend();
                }}
                className="flex items-center space-x-2"
              >
                <input
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="Type a message..."
                  className="flex-1 bg-zinc-800 hover:bg-zinc-750 focus:bg-zinc-750 text-sm text-white placeholder-gray-500 border border-zinc-700 rounded-lg px-3 py-2.5 outline-none transition focus:border-red-500 focus:ring-1 focus:ring-red-500"
                />
                <button
                  type="submit"
                  className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-red-600 hover:bg-red-700 text-white transition shadow-md hover:shadow-red-600/20"
                >
                  <Send className="h-4 w-4" />
                </button>
              </form>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
