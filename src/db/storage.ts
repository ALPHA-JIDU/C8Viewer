import { ThreadRow, Msg, Hit, AppPrefs, MediaItem } from '../types';

const DB_NAME = 'chatviewer_db';
const DB_VERSION = 1;

interface ChatDB {
  threads: ThreadRow[];
  messages: Msg[];
  prefs: AppPrefs;
  mediaCache: Record<string, string>; // uri -> dataURL or objectURL
}

class StorageManager {
  private dbPromise: Promise<IDBDatabase> | null = null;
  private memoryFallback: ChatDB = {
    threads: [],
    messages: [],
    prefs: { imported: false, me: '' },
    mediaCache: {},
  };
  private isIndexedDBAvailable = typeof indexedDB !== 'undefined';

  private initDB(): Promise<IDBDatabase> {
    if (this.dbPromise) return this.dbPromise;

    this.dbPromise = new Promise((resolve, reject) => {
      if (!this.isIndexedDBAvailable) {
        reject(new Error('IndexedDB not supported'));
        return;
      }

      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;

        if (!db.objectStoreNames.contains('threads')) {
          db.createObjectStore('threads', { keyPath: 'id' });
        }

        if (!db.objectStoreNames.contains('messages')) {
          const msgStore = db.createObjectStore('messages', { keyPath: 'id' });
          msgStore.createIndex('thread_ts', ['threadId', 'ts'], { unique: false });
          msgStore.createIndex('threadId', 'threadId', { unique: false });
        }

        if (!db.objectStoreNames.contains('prefs')) {
          db.createObjectStore('prefs', { keyPath: 'key' });
        }

        if (!db.objectStoreNames.contains('media')) {
          db.createObjectStore('media', { keyPath: 'uri' });
        }
      };

      request.onsuccess = () => resolve(request.result);
      request.onerror = () => {
        console.warn('Failed to open IndexedDB, using memory fallback', request.error);
        reject(request.error);
      };
    });

    return this.dbPromise;
  }

  async getPrefs(): Promise<AppPrefs> {
    try {
      const db = await this.initDB();
      return new Promise((resolve) => {
        const tx = db.transaction('prefs', 'readonly');
        const store = tx.objectStore('prefs');
        const req = store.get('main');
        req.onsuccess = () => {
          if (req.result && req.result.value) {
            resolve(req.result.value);
          } else {
            resolve(this.memoryFallback.prefs);
          }
        };
        req.onerror = () => resolve(this.memoryFallback.prefs);
      });
    } catch {
      return this.memoryFallback.prefs;
    }
  }

  async setPrefs(prefs: AppPrefs): Promise<void> {
    this.memoryFallback.prefs = prefs;
    try {
      const db = await this.initDB();
      return new Promise((resolve, reject) => {
        const tx = db.transaction('prefs', 'readwrite');
        const store = tx.objectStore('prefs');
        const req = store.put({ key: 'main', value: prefs });
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      });
    } catch {
      // Memory fallback saved
    }
  }

  async getThreads(): Promise<ThreadRow[]> {
    try {
      const db = await this.initDB();
      return new Promise((resolve) => {
        const tx = db.transaction('threads', 'readonly');
        const store = tx.objectStore('threads');
        const req = store.getAll();
        req.onsuccess = () => {
          const list: ThreadRow[] = req.result || [];
          // Order by lastTs DESC matching Android Db.kt
          list.sort((a, b) => b.lastTs - a.lastTs);
          resolve(list.filter((t) => t.count > 0));
        };
        req.onerror = () => resolve(this.memoryFallback.threads);
      });
    } catch {
      return this.memoryFallback.threads;
    }
  }

  /**
   * Messages are shown newest-first: offset 0 = newest message.
   */
  async getPage(threadId: number, offset: number, limit: number): Promise<Msg[]> {
    try {
      const db = await this.initDB();
      return new Promise((resolve) => {
        const tx = db.transaction('messages', 'readonly');
        const store = tx.objectStore('messages');
        const index = store.index('threadId');
        const req = index.getAll(IDBKeyRange.only(threadId));

        req.onsuccess = () => {
          const all: Msg[] = req.result || [];
          // Sort newest-first: ts DESC, id DESC
          all.sort((a, b) => {
            if (b.ts !== a.ts) return b.ts - a.ts;
            return b.id - a.id;
          });
          const slice = all.slice(offset, offset + limit);
          resolve(slice);
        };
        req.onerror = () => {
          const all = this.memoryFallback.messages.filter((m) => m.threadId === threadId);
          all.sort((a, b) => (b.ts !== a.ts ? b.ts - a.ts : b.id - a.id));
          resolve(all.slice(offset, offset + limit));
        };
      });
    } catch {
      const all = this.memoryFallback.messages.filter((m) => m.threadId === threadId);
      all.sort((a, b) => (b.ts !== a.ts ? b.ts - a.ts : b.id - a.id));
      return all.slice(offset, offset + limit);
    }
  }

  /**
   * Index of a message in the newest-first list (used to jump to a search result).
   */
  async positionOf(threadId: number, msgId: number): Promise<number> {
    try {
      const db = await this.initDB();
      return new Promise((resolve) => {
        const tx = db.transaction('messages', 'readonly');
        const store = tx.objectStore('messages');
        const index = store.index('threadId');
        const req = index.getAll(IDBKeyRange.only(threadId));

        req.onsuccess = () => {
          const all: Msg[] = req.result || [];
          all.sort((a, b) => {
            if (b.ts !== a.ts) return b.ts - a.ts;
            return b.id - a.id;
          });
          const pos = all.findIndex((m) => m.id === msgId);
          resolve(pos >= 0 ? pos : 0);
        };
        req.onerror = () => resolve(0);
      });
    } catch {
      const all = this.memoryFallback.messages.filter((m) => m.threadId === threadId);
      all.sort((a, b) => (b.ts !== a.ts ? b.ts - a.ts : b.id - a.id));
      const pos = all.findIndex((m) => m.id === msgId);
      return pos >= 0 ? pos : 0;
    }
  }

  /**
   * Search messages by query tokens matching Android FTS4 search.
   */
  async search(query: string, threadId: number | null): Promise<Hit[]> {
    const trimmed = query.trim().toLowerCase();
    if (!trimmed) return [];

    const tokens = trimmed.split(/[^a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF]+/).filter(Boolean);
    if (tokens.length === 0) return [];

    try {
      const threads = await this.getThreads();
      const threadMap = new Map<number, string>();
      threads.forEach((t) => threadMap.set(t.id, t.title));

      const db = await this.initDB();
      return new Promise((resolve) => {
        const tx = db.transaction('messages', 'readonly');
        const store = tx.objectStore('messages');
        const req = threadId !== null
          ? store.index('threadId').getAll(IDBKeyRange.only(threadId))
          : store.getAll();

        req.onsuccess = () => {
          const all: Msg[] = req.result || [];
          const hits: Hit[] = [];

          for (const m of all) {
            if (!m.content) continue;
            const lowerContent = m.content.toLowerCase();
            // Check if all tokens match (like FTS match)
            const allMatch = tokens.every((token) => lowerContent.includes(token));
            if (allMatch) {
              hits.push({
                msgId: m.id,
                threadId: m.threadId,
                title: threadMap.get(m.threadId) || 'Chat',
                sender: m.sender,
                ts: m.ts,
                content: m.content,
              });
              if (hits.length >= 300) break; // Limit 300 matching Android
            }
          }

          // Sort by ts DESC
          hits.sort((a, b) => b.ts - a.ts);
          resolve(hits);
        };
        req.onerror = () => resolve([]);
      });
    } catch {
      const hits: Hit[] = [];
      const threadMap = new Map<number, string>();
      this.memoryFallback.threads.forEach((t) => threadMap.set(t.id, t.title));

      for (const m of this.memoryFallback.messages) {
        if (threadId !== null && m.threadId !== threadId) continue;
        if (!m.content) continue;
        const lower = m.content.toLowerCase();
        if (tokens.every((t) => lower.includes(t))) {
          hits.push({
            msgId: m.id,
            threadId: m.threadId,
            title: threadMap.get(m.threadId) || 'Chat',
            sender: m.sender,
            ts: m.ts,
            content: m.content,
          });
          if (hits.length >= 300) break;
        }
      }
      hits.sort((a, b) => b.ts - a.ts);
      return hits;
    }
  }

  async saveMedia(uri: string, blobUrl: string): Promise<void> {
    this.memoryFallback.mediaCache[uri] = blobUrl;
    try {
      const db = await this.initDB();
      const tx = db.transaction('media', 'readwrite');
      tx.objectStore('media').put({ uri, blobUrl });
    } catch {
      // fallback
    }
  }

  async getMedia(uri: string): Promise<string | null> {
    if (this.memoryFallback.mediaCache[uri]) {
      return this.memoryFallback.mediaCache[uri];
    }
    try {
      const db = await this.initDB();
      return new Promise((resolve) => {
        const tx = db.transaction('media', 'readonly');
        const req = tx.objectStore('media').get(uri);
        req.onsuccess = () => resolve(req.result ? req.result.blobUrl : null);
        req.onerror = () => resolve(null);
      });
    } catch {
      return null;
    }
  }

  async saveImportData(
    threads: ThreadRow[],
    messages: Msg[],
    me: string,
    mediaMap?: Record<string, string>
  ): Promise<void> {
    this.memoryFallback = {
      threads,
      messages,
      prefs: { imported: true, me, lastImportTs: Date.now() },
      mediaCache: mediaMap || {},
    };

    try {
      const db = await this.initDB();
      return new Promise((resolve, reject) => {
        const tx = db.transaction(['threads', 'messages', 'prefs', 'media'], 'readwrite');
        const threadStore = tx.objectStore('threads');
        const msgStore = tx.objectStore('messages');
        const prefStore = tx.objectStore('prefs');
        const mediaStore = tx.objectStore('media');

        threadStore.clear();
        msgStore.clear();
        mediaStore.clear();

        threads.forEach((t) => threadStore.put(t));
        messages.forEach((m) => msgStore.put(m));
        prefStore.put({ key: 'main', value: { imported: true, me, lastImportTs: Date.now() } });

        if (mediaMap) {
          Object.entries(mediaMap).forEach(([uri, blobUrl]) => {
            mediaStore.put({ uri, blobUrl });
          });
        }

        tx.oncomplete = () => resolve();
        tx.onerror = () => {
          console.warn('Transaction error during save, falling back to memory', tx.error);
          resolve();
        };
      });
    } catch (e) {
      console.warn('IndexedDB write error, saved in memory', e);
    }
  }

  async clearAll(): Promise<void> {
    this.memoryFallback = {
      threads: [],
      messages: [],
      prefs: { imported: false, me: '' },
      mediaCache: {},
    };

    try {
      const db = await this.initDB();
      const tx = db.transaction(['threads', 'messages', 'prefs', 'media'], 'readwrite');
      tx.objectStore('threads').clear();
      tx.objectStore('messages').clear();
      tx.objectStore('prefs').clear();
      tx.objectStore('media').clear();
    } catch {
      // fallback cleared
    }
  }
}

export const dbStorage = new StorageManager();
