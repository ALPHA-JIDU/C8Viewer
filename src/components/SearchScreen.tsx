import React, { useEffect, useState } from 'react';
import { ArrowLeft, Search as SearchIcon, MessageSquare } from 'lucide-react';
import { ThreadRow, Hit } from '../types';
import { dbStorage } from '../db/storage';
import { fmtDate } from '../utils/encoding';

interface SearchScreenProps {
  scope: ThreadRow | null;
  onBack: () => void;
  onOpen: (hit: Hit) => void;
}

export const SearchScreen: React.FC<SearchScreenProps> = ({
  scope,
  onBack,
  onOpen,
}) => {
  const [query, setQuery] = useState('');
  const [hits, setHits] = useState<Hit[]>([]);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    const trimmed = query.trim();
    if (!trimmed) {
      setHits([]);
      setSearching(false);
      return;
    }

    setSearching(true);
    const timer = setTimeout(async () => {
      try {
        const results = await dbStorage.search(trimmed, scope ? scope.id : null);
        setHits(results);
      } finally {
        setSearching(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [query, scope]);

  return (
    <div className="flex flex-col h-screen max-w-3xl mx-auto bg-white dark:bg-slate-900 border-x border-slate-200 dark:border-slate-800 shadow-sm">
      {/* Top Header */}
      <header className="px-3 py-3 border-b border-slate-200 dark:border-slate-800 flex items-center gap-2">
        <button
          onClick={onBack}
          className="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-700 dark:text-slate-200 transition"
          title="Back"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-bold text-slate-900 dark:text-slate-100 truncate flex-1">
          {scope ? `Search in ${scope.title}` : 'Search all chats'}
        </h2>
      </header>

      {/* Query input */}
      <div className="p-4 border-b border-slate-100 dark:border-slate-800/80">
        <div className="relative">
          <SearchIcon className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Type a word"
            className="w-full pl-9 pr-4 py-2.5 bg-slate-100 dark:bg-slate-800/90 text-sm rounded-xl border border-transparent focus:border-blue-500 focus:bg-white dark:focus:bg-slate-900 outline-none transition text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
          />
          {query && (
            <button
              onClick={() => setQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              Clear
            </button>
          )}
        </div>

        {/* Results count status */}
        {query.trim().length > 0 && (
          <div className="mt-2 text-xs font-medium text-slate-500 dark:text-slate-400 px-1 flex items-center justify-between">
            <span>
              {searching
                ? 'Searching...'
                : `${hits.length}${hits.length >= 300 ? '+' : ''} results`}
            </span>
          </div>
        )}
      </div>

      {/* Search results list */}
      <div className="flex-1 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800/60">
        {query.trim().length > 0 && hits.length === 0 && !searching && (
          <div className="flex flex-col items-center justify-center h-48 text-center px-4">
            <MessageSquare className="w-8 h-8 text-slate-300 dark:text-slate-600 mb-2" />
            <p className="text-xs text-slate-500 dark:text-slate-400">
              No messages found matching &quot;{query}&quot;
            </p>
          </div>
        )}

        {hits.map((hit) => {
          const headerSubtitle = `${scope ? '' : `${hit.title}  •  `}${hit.sender}  •  ${fmtDate(hit.ts)}`;

          return (
            <div
              key={`${hit.threadId}-${hit.msgId}`}
              onClick={() => onOpen(hit)}
              className="px-4 py-3 hover:bg-slate-50 dark:hover:bg-slate-800/40 cursor-pointer transition select-none active:bg-slate-100 dark:active:bg-slate-800/70"
            >
              <div className="text-xs font-semibold text-blue-600 dark:text-blue-400 mb-1 truncate">
                {headerSubtitle}
              </div>
              <p className="text-sm text-slate-800 dark:text-slate-200 line-clamp-3 leading-snug">
                {hit.content}
              </p>
            </div>
          );
        })}
      </div>
    </div>
  );
};
