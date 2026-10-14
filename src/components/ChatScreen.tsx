import React, { useEffect, useRef, useState, useCallback } from 'react';
import { ArrowLeft, Search, ChevronDown, ChevronUp, ExternalLink, Play, Volume2, FileText, ImageOff } from 'lucide-react';
import { ThreadRow, Msg, MediaItem } from '../types';
import { dbStorage } from '../db/storage';
import { fmtDate, fmtTime, dayOf } from '../utils/encoding';
import { ImageViewerModal } from './ImageViewerModal';

interface ChatScreenProps {
  thread: ThreadRow;
  focusId: number | null;
  me: string;
  onBack: () => void;
  onSearch: () => void;
}

const PAGE_SIZE = 60;

export const ChatScreen: React.FC<ChatScreenProps> = ({
  thread,
  focusId,
  me,
  onBack,
  onSearch,
}) => {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [loading, setLoading] = useState(true);
  const [hasMoreOlder, setHasMoreOlder] = useState(false);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const focusedItemRef = useRef<HTMLDivElement>(null);
  const offsetRef = useRef(0);

  // Load initial messages
  const loadInitial = useCallback(async () => {
    setLoading(true);
    try {
      if (focusId) {
        // Find position of focusId
        const pos = await dbStorage.positionOf(thread.id, focusId);
        // Load window around pos
        const startOffset = Math.max(0, pos - 20);
        const fetched = await dbStorage.getPage(thread.id, startOffset, PAGE_SIZE);
        setMessages(fetched);
        offsetRef.current = startOffset + fetched.length;
        setHasMoreOlder(offsetRef.current < thread.count);
      } else {
        const fetched = await dbStorage.getPage(thread.id, 0, PAGE_SIZE);
        setMessages(fetched);
        offsetRef.current = fetched.length;
        setHasMoreOlder(fetched.length < thread.count);
      }
    } finally {
      setLoading(false);
    }
  }, [thread.id, thread.count, focusId]);

  useEffect(() => {
    loadInitial();
  }, [loadInitial]);

  // Scroll to focused message or bottom after loading
  useEffect(() => {
    if (!loading && messages.length > 0) {
      if (focusId && focusedItemRef.current) {
        focusedItemRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }, [loading, focusId, messages]);

  const loadMoreOlder = async () => {
    if (offsetRef.current >= thread.count) return;
    const more = await dbStorage.getPage(thread.id, offsetRef.current, PAGE_SIZE);
    if (more.length > 0) {
      setMessages((prev) => [...prev, ...more]);
      offsetRef.current += more.length;
      setHasMoreOlder(offsetRef.current < thread.count);
    }
  };

  const scrollToOldest = async () => {
    // In Android: listState.scrollToItem(source.count - 1)
    // Here we load the oldest slice (e.g. offset = thread.count - PAGE_SIZE)
    setLoading(true);
    const start = Math.max(0, thread.count - PAGE_SIZE);
    const oldestMsgs = await dbStorage.getPage(thread.id, start, PAGE_SIZE);
    setMessages(oldestMsgs);
    offsetRef.current = thread.count;
    setHasMoreOlder(false);
    setLoading(false);
    if (containerRef.current) {
      containerRef.current.scrollTop = 0;
    }
  };

  const scrollToLatest = async () => {
    setLoading(true);
    const latestMsgs = await dbStorage.getPage(thread.id, 0, PAGE_SIZE);
    setMessages(latestMsgs);
    offsetRef.current = latestMsgs.length;
    setHasMoreOlder(latestMsgs.length < thread.count);
    setLoading(false);
    if (containerRef.current) {
      containerRef.current.scrollTop = 0;
    }
  };

  return (
    <div className="flex flex-col h-screen max-w-3xl mx-auto bg-slate-50 dark:bg-slate-950 border-x border-slate-200 dark:border-slate-800 shadow-sm">
      {/* Top Header */}
      <header className="px-3 py-2.5 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-2 shrink-0 z-10">
        <div className="flex items-center gap-1.5 min-w-0">
          <button
            onClick={onBack}
            className="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-700 dark:text-slate-200 transition"
            title="Back to chats"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>

          <div className="min-w-0">
            <h2 className="text-sm font-bold text-slate-900 dark:text-slate-100 truncate">
              {thread.title}
            </h2>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              {thread.count.toLocaleString()} messages
            </p>
          </div>
        </div>

        {/* Action Buttons: Oldest, Latest, Search */}
        <div className="flex items-center gap-1 shrink-0">
          <button
            onClick={scrollToOldest}
            className="px-2.5 py-1 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-md transition flex items-center gap-1"
            title="Jump to oldest messages"
          >
            <ChevronUp className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Oldest</span>
          </button>
          <button
            onClick={scrollToLatest}
            className="px-2.5 py-1 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-md transition flex items-center gap-1"
            title="Jump to latest messages"
          >
            <ChevronDown className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Latest</span>
          </button>
          <button
            onClick={onSearch}
            className="px-2.5 py-1 text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-md transition flex items-center gap-1"
            title="Search this chat"
          >
            <Search className="w-3.5 h-3.5" />
            <span>Search</span>
          </button>
        </div>
      </header>

      {/* Messages Scroll Area */}
      {/* Messages are stored newest-first in the array (offset 0 = newest) */}
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto p-3 sm:p-4 space-y-3"
      >
        {loading ? (
          <div className="flex items-center justify-center h-48 text-xs text-slate-400">
            Loading messages...
          </div>
        ) : messages.length === 0 ? (
          <div className="flex items-center justify-center h-48 text-xs text-slate-400">
            No messages found
          </div>
        ) : (
          <>
            {/* Messages rendered */}
            {messages.map((m, idx) => {
              const older = messages[idx + 1];
              const isMine = m.sender === me;
              const isGroup = thread.people > 2;
              const isHighlighted = focusId !== null && m.id === focusId;
              const showDateHeader = !older || dayOf(older.ts) !== dayOf(m.ts);

              return (
                <div
                  key={m.id}
                  ref={isHighlighted ? focusedItemRef : undefined}
                  className="flex flex-col"
                >
                  {/* Day separator header */}
                  {showDateHeader && (
                    <div className="flex justify-center my-3">
                      <span className="bg-slate-200/80 dark:bg-slate-800 text-slate-600 dark:text-slate-400 text-[11px] font-medium px-3 py-1 rounded-full shadow-2xs">
                        {fmtDate(m.ts)}
                      </span>
                    </div>
                  )}

                  <div
                    className={`flex flex-col ${
                      isMine ? 'items-end' : 'items-start'
                    }`}
                  >
                    {/* Sender name for group chats */}
                    {isGroup && !isMine && (
                      <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 ml-2 mb-1">
                        {m.sender}
                      </span>
                    )}

                    {/* Message Bubble */}
                    <div
                      className={`max-w-[85%] sm:max-w-[420px] rounded-2xl px-3.5 py-2.5 transition-all shadow-xs ${
                        isHighlighted
                          ? 'bg-amber-200 dark:bg-amber-900/80 ring-2 ring-amber-400 text-amber-950 dark:text-amber-100'
                          : isMine
                          ? 'bg-blue-600 text-white rounded-br-xs'
                          : 'bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 border border-slate-200/70 dark:border-slate-700/60 rounded-bl-xs'
                      }`}
                    >
                      {/* Unsent marker */}
                      {m.unsent && (
                        <p className="italic text-xs opacity-75 mb-1">Message unsent</p>
                      )}

                      {/* Media items */}
                      {m.media.map((item, mIdx) => (
                        <div key={mIdx} className="mb-2">
                          <MediaItemView
                            item={item}
                            isMine={isMine}
                            onImageClick={(src) => setSelectedImage(src)}
                          />
                        </div>
                      ))}

                      {/* Text content */}
                      {m.content && (
                        <p className="text-sm whitespace-pre-wrap break-words leading-relaxed">
                          {m.content}
                        </p>
                      )}

                      {/* Link */}
                      {m.link && (
                        <a
                          href={m.link}
                          target="_blank"
                          rel="noopener noreferrer"
                          className={`mt-1.5 inline-flex items-center gap-1 text-xs underline font-medium truncate max-w-full ${
                            isMine ? 'text-blue-100 hover:text-white' : 'text-blue-600 dark:text-blue-400'
                          }`}
                        >
                          <ExternalLink className="w-3 h-3 shrink-0" />
                          <span className="truncate">{m.link}</span>
                        </a>
                      )}

                      {/* Timestamp */}
                      <div
                        className={`text-[10px] mt-1 text-right ${
                          isMine
                            ? 'text-blue-100/80'
                            : 'text-slate-400 dark:text-slate-500'
                        }`}
                      >
                        {fmtTime(m.ts)}
                      </div>
                    </div>

                    {/* Reactions */}
                    {m.reactions && (
                      <div className="flex flex-wrap gap-1 mt-1 px-1 text-[11px] text-slate-500 dark:text-slate-400">
                        {m.reactions.split('\n').map((react, rIdx) => (
                          <span
                            key={rIdx}
                            className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700/60 rounded-full px-2 py-0.5 text-[11px] shadow-2xs"
                          >
                            {react}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}

            {/* Load more button */}
            {hasMoreOlder && (
              <div className="flex justify-center pt-2 pb-4">
                <button
                  onClick={loadMoreOlder}
                  className="px-4 py-1.5 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-full transition shadow-xs"
                >
                  Load older messages...
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Full screen image viewer */}
      {selectedImage && (
        <ImageViewerModal
          src={selectedImage}
          onClose={() => setSelectedImage(null)}
        />
      )}
    </div>
  );
};

interface MediaItemViewProps {
  item: MediaItem;
  isMine: boolean;
  onImageClick: (src: string) => void;
}

const MediaItemView: React.FC<MediaItemViewProps> = ({ item, isMine, onImageClick }) => {
  const [imgError, setImgError] = useState(false);
  const src = item.blobUrl || item.uri;

  if (item.type === 'photo' || item.type === 'gif' || item.type === 'sticker') {
    const isSticker = item.type === 'sticker';

    if (imgError) {
      return (
        <div className="w-48 h-32 rounded-xl bg-slate-100 dark:bg-slate-800 flex flex-col items-center justify-center text-xs text-slate-400 p-2 text-center border border-slate-200 dark:border-slate-700">
          <ImageOff className="w-6 h-6 mb-1 text-slate-300 dark:text-slate-600" />
          <span>{item.type} not available</span>
        </div>
      );
    }

    return (
      <img
        src={src}
        alt={item.type}
        onError={() => setImgError(true)}
        onClick={() => onImageClick(src)}
        className={`rounded-xl object-cover cursor-pointer hover:opacity-95 transition ${
          isSticker ? 'max-w-[140px] max-h-[140px]' : 'max-w-[240px] max-h-[220px]'
        }`}
      />
    );
  }

  if (item.type === 'video') {
    return (
      <div className="rounded-xl overflow-hidden bg-black/10 dark:bg-white/10 p-2">
        {src.startsWith('blob:') || src.startsWith('http') ? (
          <video src={src} controls className="max-w-[240px] rounded-lg" />
        ) : (
          <div className="flex items-center gap-2 p-2 text-xs font-medium">
            <Play className="w-4 h-4" />
            <span>Video file: {item.uri.split('/').pop()}</span>
          </div>
        )}
      </div>
    );
  }

  if (item.type === 'audio') {
    return (
      <div className="rounded-xl p-2 bg-black/10 dark:bg-white/10 flex items-center gap-2">
        <Volume2 className="w-4 h-4 shrink-0" />
        {src.startsWith('blob:') || src.startsWith('http') ? (
          <audio src={src} controls className="h-8 max-w-[200px]" />
        ) : (
          <span className="text-xs">Voice note: {item.uri.split('/').pop()}</span>
        )}
      </div>
    );
  }

  return (
    <div
      className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium ${
        isMine ? 'bg-white/20 text-white' : 'bg-slate-100 dark:bg-slate-700/60 text-slate-700 dark:text-slate-200'
      }`}
    >
      <FileText className="w-4 h-4 shrink-0" />
      <span className="truncate">{item.uri.split('/').pop() || 'Attachment'}</span>
    </div>
  );
};
