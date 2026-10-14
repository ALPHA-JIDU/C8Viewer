import React, { useMemo, useState } from 'react';
import { Search, RefreshCw, MessageSquare } from 'lucide-react';
import { ThreadRow } from '../types';
import { fmtDate } from '../utils/encoding';

interface HomeScreenProps {
  threads: ThreadRow[];
  onOpen: (thread: ThreadRow) => void;
  onSearch: () => void;
  onReimport: () => void;
}

// Consistent colors for avatar initial circles
const AVATAR_COLORS = [
  'bg-blue-100 text-blue-700 dark:bg-blue-900/60 dark:text-blue-300',
  'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/60 dark:text-indigo-300',
  'bg-violet-100 text-violet-700 dark:bg-violet-900/60 dark:text-violet-300',
  'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/60 dark:text-emerald-300',
  'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300',
  'bg-rose-100 text-rose-700 dark:bg-rose-900/60 dark:text-rose-300',
  'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/60 dark:text-cyan-300',
];

function getAvatarColor(title: string): string {
  let hash = 0;
  for (let i = 0; i < title.length; i++) {
    hash = title.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  threads,
  onOpen,
  onSearch,
  onReimport,
}) => {
  const [filter, setFilter] = useState('');

  const filteredThreads = useMemo(() => {
    const trimmed = filter.trim().toLowerCase();
    if (!trimmed) return threads;
    return threads.filter((t) => {
      const matchTitle = t.title.toLowerCase().includes(trimmed);
      const matchParticipant = t.participants?.some((p) => p.toLowerCase().includes(trimmed));
      return matchTitle || matchParticipant;
    });
  }, [threads, filter]);

  return (
    <div className="flex flex-col h-screen max-w-3xl mx-auto bg-white dark:bg-slate-900 border-x border-slate-200 dark:border-slate-800 shadow-sm">
      {/* Top Bar */}
      <header className="px-4 pt-4 pb-2 border-b border-slate-100 dark:border-slate-800/80">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Chats</h1>
            <span className="text-xs bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 font-semibold px-2 py-0.5 rounded-full">
              {threads.length}
            </span>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={onSearch}
              className="text-sm font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 px-3 py-1.5 rounded-lg transition flex items-center gap-1.5"
            >
              <Search className="w-4 h-4" />
              <span>Search all</span>
            </button>
            <button
              onClick={onReimport}
              className="text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 px-3 py-1.5 rounded-lg transition flex items-center gap-1.5"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Re-import</span>
            </button>
          </div>
        </div>

        {/* Search / Filter input */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Find a person or group"
            className="w-full pl-9 pr-4 py-2 bg-slate-100 dark:bg-slate-800/90 text-sm rounded-xl border border-transparent focus:border-blue-500 focus:bg-white dark:focus:bg-slate-900 outline-none transition text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
          />
          {filter && (
            <button
              onClick={() => setFilter('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              Clear
            </button>
          )}
        </div>
      </header>

      {/* Threads List */}
      <div className="flex-1 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800/60">
        {filteredThreads.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 text-center px-4">
            <MessageSquare className="w-10 h-10 text-slate-300 dark:text-slate-600 mb-2" />
            <p className="text-sm text-slate-500 dark:text-slate-400 font-medium">
              {filter ? 'No chats found matching your search' : 'No chats imported yet'}
            </p>
          </div>
        ) : (
          filteredThreads.map((thread) => {
            const initial = thread.title.trim().slice(0, 1).toUpperCase() || '?';
            const colorClass = getAvatarColor(thread.title);

            return (
              <div
                key={thread.id}
                onClick={() => onOpen(thread)}
                className="flex items-center gap-3.5 px-4 py-3 hover:bg-slate-50 dark:hover:bg-slate-800/40 cursor-pointer transition select-none active:bg-slate-100 dark:active:bg-slate-800/70"
              >
                {/* Circle Avatar */}
                <div
                  className={`w-12 h-12 rounded-full flex items-center justify-center font-bold text-base shrink-0 shadow-xs ${colorClass}`}
                >
                  {initial}
                </div>

                {/* Title & Preview */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2 mb-0.5">
                    <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100 truncate">
                      {thread.title}
                    </h2>
                    <span className="text-[11px] text-slate-400 shrink-0 font-medium">
                      {fmtDate(thread.lastTs)}
                    </span>
                  </div>

                  <div className="flex items-center justify-between gap-2">
                    <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                      {thread.preview || '(media)'}
                    </p>
                    <span className="text-[11px] text-slate-400 dark:text-slate-500 font-medium shrink-0">
                      {thread.count.toLocaleString()}
                    </span>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
