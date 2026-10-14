import JSZip from 'jszip';
import { ThreadRow, Msg, MediaItem } from '../types';
import { fixEncoding } from './encoding';

export interface ImportProgress {
  running: boolean;
  status: string;
  chatsCount: number;
  totalMsgs: number;
  error: string | null;
}

const SECTIONS = new Set([
  'inbox',
  'archived_threads',
  'filtered_threads',
  'message_requests',
  'e2ee_cutover',
]);

const FILE_RE = /message_(\d+)\.json$/i;

interface RawMessage {
  sender_name?: string;
  timestamp_ms?: number;
  content?: string;
  is_unsent?: boolean;
  photos?: Array<{ uri: string }>;
  videos?: Array<{ uri: string }>;
  audio_files?: Array<{ uri: string }>;
  gifs?: Array<{ uri: string }>;
  files?: Array<{ uri: string }>;
  sticker?: { uri: string };
  share?: { link?: string };
  reactions?: Array<{ reaction?: string; actor?: string }>;
}

interface RawExportFile {
  title?: string;
  participants?: Array<{ name?: string }>;
  messages?: RawMessage[];
}

export interface ParsedChatData {
  threads: ThreadRow[];
  messages: Msg[];
  me: string;
  mediaMap: Record<string, string>;
}

export class ChatImporter {
  /**
   * Import from FileList (when user picks a folder using webkitdirectory)
   */
  static async importFromFiles(
    files: FileList | File[],
    onProgress: (status: string, currentChat: number, totalChats: number, totalMsgs: number) => void
  ): Promise<ParsedChatData> {
    const fileArray = Array.from(files);
    onProgress('Scanning selected folder for Facebook messages...', 0, 0, 0);

    // Group files by thread folder
    // E.g.: .../messages/inbox/friendname_12345/message_1.json
    // Also store media files (photos, mp4, etc.) to create object URLs
    const threadJsonFiles = new Map<string, Array<{ file: File; index: number; name: string }>>();
    const mediaMap: Record<string, string> = {};

    for (const f of fileArray) {
      const path = (f.webkitRelativePath || f.name).replace(/\\/g, '/');

      // Check if media
      const isMedia = /\.(jpg|jpeg|png|gif|webp|mp4|webm|mp3|m4a|ogg|wav)$/i.test(path);
      if (isMedia) {
        const objectUrl = URL.createObjectURL(f);
        // Store both full path and relative segments
        mediaMap[path] = objectUrl;
        const normalized = path.replace(/^.*?(your_facebook_activity\/|messages\/)/i, '$1');
        mediaMap[normalized] = objectUrl;
        // Also by last 2-3 segments (e.g. photos/123.jpg)
        const parts = path.split('/');
        if (parts.length >= 2) {
          mediaMap[parts.slice(-2).join('/')] = objectUrl;
        }
        if (parts.length >= 3) {
          mediaMap[parts.slice(-3).join('/')] = objectUrl;
        }
      }

      // Check if message JSON
      const match = path.match(FILE_RE);
      if (match) {
        // Find parent thread folder name
        const parts = path.split('/');
        const threadFolder = parts.length > 1 ? parts[parts.length - 2] : 'chat';
        const fileIndex = parseInt(match[1], 10);

        if (!threadJsonFiles.has(threadFolder)) {
          threadJsonFiles.set(threadFolder, []);
        }
        threadJsonFiles.get(threadFolder)!.push({
          file: f,
          index: fileIndex,
          name: parts[parts.length - 1],
        });
      }
    }

    if (threadJsonFiles.size === 0) {
      throw new Error(
        "Could not find any 'message_*.json' files in the selected folder. " +
          "Please make sure you selected the extracted Meta export folder containing 'your_facebook_activity' or 'messages'."
      );
    }

    return this.parseDiscoveredThreads(threadJsonFiles, mediaMap, onProgress);
  }

  /**
   * Import directly from a ZIP file
   */
  static async importFromZip(
    zipFile: File,
    onProgress: (status: string, currentChat: number, totalChats: number, totalMsgs: number) => void
  ): Promise<ParsedChatData> {
    onProgress('Reading ZIP archive (this may take a moment)...', 0, 0, 0);

    const zip = await JSZip.loadAsync(zipFile);
    const threadJsonFiles = new Map<string, Array<{ fileContent: () => Promise<string>; index: number; name: string }>>();
    const mediaMap: Record<string, string> = {};

    const entries = Object.keys(zip.files);
    onProgress(`Analyzing ${entries.length} files in archive...`, 0, 0, 0);

    for (const path of entries) {
      const entry = zip.files[path];
      if (entry.dir) continue;

      const normalizedPath = path.replace(/\\/g, '/');

      // Check if media
      const isMedia = /\.(jpg|jpeg|png|gif|webp|mp4|webm|mp3|m4a|ogg|wav)$/i.test(normalizedPath);
      if (isMedia) {
        // Load media asynchronously when requested or as blob
        try {
          const blob = await entry.async('blob');
          const objectUrl = URL.createObjectURL(blob);
          mediaMap[normalizedPath] = objectUrl;
          const normalized = normalizedPath.replace(/^.*?(your_facebook_activity\/|messages\/)/i, '$1');
          mediaMap[normalized] = objectUrl;
          const parts = normalizedPath.split('/');
          if (parts.length >= 2) mediaMap[parts.slice(-2).join('/')] = objectUrl;
          if (parts.length >= 3) mediaMap[parts.slice(-3).join('/')] = objectUrl;
        } catch {
          // Skip corrupt media
        }
      }

      // Check if message JSON
      const match = normalizedPath.match(FILE_RE);
      if (match) {
        const parts = normalizedPath.split('/');
        const threadFolder = parts.length > 1 ? parts[parts.length - 2] : 'chat';
        const fileIndex = parseInt(match[1], 10);

        if (!threadJsonFiles.has(threadFolder)) {
          threadJsonFiles.set(threadFolder, []);
        }
        threadJsonFiles.get(threadFolder)!.push({
          fileContent: () => entry.async('string'),
          index: fileIndex,
          name: parts[parts.length - 1],
        });
      }
    }

    if (threadJsonFiles.size === 0) {
      throw new Error(
        "No chat files ('message_*.json') were found in this ZIP archive. " +
          "Please verify that this is a downloaded Facebook/Meta archive with Messages selected."
      );
    }

    return this.parseDiscoveredZipThreads(threadJsonFiles, mediaMap, onProgress);
  }

  private static async parseDiscoveredThreads(
    threadJsonFiles: Map<string, Array<{ file: File; index: number; name: string }>>,
    mediaMap: Record<string, string>,
    onProgress: (status: string, currentChat: number, totalChats: number, totalMsgs: number) => void
  ): Promise<ParsedChatData> {
    const threadEntries = Array.from(threadJsonFiles.entries());
    const totalThreads = threadEntries.length;

    const threads: ThreadRow[] = [];
    const allMessages: Msg[] = [];
    const nameCounts = new Map<string, number>();

    let currentMsgId = 1;
    let threadIdCounter = 1;
    let totalMessagesCount = 0;

    for (let i = 0; i < totalThreads; i++) {
      const [folderName, jsonList] = threadEntries[i];
      // Sort message_1.json, message_2.json
      jsonList.sort((a, b) => a.index - b.index);

      let chatTitle = folderName;
      const participantsSet = new Set<string>();
      const threadMessages: Msg[] = [];

      for (const item of jsonList) {
        try {
          const text = await item.file.text();
          const parsed: RawExportFile = JSON.parse(text);

          if (parsed.title) {
            chatTitle = fixEncoding(parsed.title);
          }

          if (Array.isArray(parsed.participants)) {
            for (const p of parsed.participants) {
              if (p?.name) {
                const fixed = fixEncoding(p.name);
                if (fixed) participantsSet.add(fixed);
              }
            }
          }

          if (Array.isArray(parsed.messages)) {
            for (const m of parsed.messages) {
              const msg = this.convertRawMessage(m, threadIdCounter, currentMsgId++, mediaMap);
              threadMessages.push(msg);
              totalMessagesCount++;
            }
          }
        } catch (e) {
          console.warn(`Failed to parse ${item.name}`, e);
        }
      }

      onProgress(
        `Importing chat ${i + 1} of ${totalThreads}\n${chatTitle}\n${totalMessagesCount.toLocaleString()} messages so far`,
        i + 1,
        totalThreads,
        totalMessagesCount
      );

      const participants = Array.from(participantsSet);
      participants.forEach((p) => {
        nameCounts.set(p, (nameCounts.get(p) || 0) + 1);
      });

      // Find last timestamp and preview
      threadMessages.sort((a, b) => b.ts - a.ts);
      const lastTs = threadMessages.length > 0 ? threadMessages[0].ts : 0;
      const preview =
        threadMessages.find((m) => m.content && m.content.trim().length > 0)?.content ||
        (threadMessages.length > 0 && threadMessages[0].media.length > 0 ? '(media)' : '');

      threads.push({
        id: threadIdCounter,
        title: chatTitle || folderName,
        folder: folderName,
        people: Math.max(participants.length, 2),
        count: threadMessages.length,
        lastTs,
        preview: preview.slice(0, 100),
        participants,
      });

      allMessages.push(...threadMessages);
      threadIdCounter++;
    }

    // Determine "me" = participant appearing in the highest number of chats
    let me = '';
    let maxCount = -1;
    nameCounts.forEach((count, name) => {
      if (count > maxCount) {
        maxCount = count;
        me = name;
      }
    });

    threads.sort((a, b) => b.lastTs - a.lastTs);

    return {
      threads,
      messages: allMessages,
      me,
      mediaMap,
    };
  }

  private static async parseDiscoveredZipThreads(
    threadJsonFiles: Map<string, Array<{ fileContent: () => Promise<string>; index: number; name: string }>>,
    mediaMap: Record<string, string>,
    onProgress: (status: string, currentChat: number, totalChats: number, totalMsgs: number) => void
  ): Promise<ParsedChatData> {
    const threadEntries = Array.from(threadJsonFiles.entries());
    const totalThreads = threadEntries.length;

    const threads: ThreadRow[] = [];
    const allMessages: Msg[] = [];
    const nameCounts = new Map<string, number>();

    let currentMsgId = 1;
    let threadIdCounter = 1;
    let totalMessagesCount = 0;

    for (let i = 0; i < totalThreads; i++) {
      const [folderName, jsonList] = threadEntries[i];
      jsonList.sort((a, b) => a.index - b.index);

      let chatTitle = folderName;
      const participantsSet = new Set<string>();
      const threadMessages: Msg[] = [];

      for (const item of jsonList) {
        try {
          const text = await item.fileContent();
          const parsed: RawExportFile = JSON.parse(text);

          if (parsed.title) {
            chatTitle = fixEncoding(parsed.title);
          }

          if (Array.isArray(parsed.participants)) {
            for (const p of parsed.participants) {
              if (p?.name) {
                const fixed = fixEncoding(p.name);
                if (fixed) participantsSet.add(fixed);
              }
            }
          }

          if (Array.isArray(parsed.messages)) {
            for (const m of parsed.messages) {
              const msg = this.convertRawMessage(m, threadIdCounter, currentMsgId++, mediaMap);
              threadMessages.push(msg);
              totalMessagesCount++;
            }
          }
        } catch (e) {
          console.warn(`Failed to parse zip item ${item.name}`, e);
        }
      }

      onProgress(
        `Importing chat ${i + 1} of ${totalThreads}\n${chatTitle}\n${totalMessagesCount.toLocaleString()} messages so far`,
        i + 1,
        totalThreads,
        totalMessagesCount
      );

      const participants = Array.from(participantsSet);
      participants.forEach((p) => {
        nameCounts.set(p, (nameCounts.get(p) || 0) + 1);
      });

      threadMessages.sort((a, b) => b.ts - a.ts);
      const lastTs = threadMessages.length > 0 ? threadMessages[0].ts : 0;
      const preview =
        threadMessages.find((m) => m.content && m.content.trim().length > 0)?.content ||
        (threadMessages.length > 0 && threadMessages[0].media.length > 0 ? '(media)' : '');

      threads.push({
        id: threadIdCounter,
        title: chatTitle || folderName,
        folder: folderName,
        people: Math.max(participants.length, 2),
        count: threadMessages.length,
        lastTs,
        preview: preview.slice(0, 100),
        participants,
      });

      allMessages.push(...threadMessages);
      threadIdCounter++;
    }

    let me = '';
    let maxCount = -1;
    nameCounts.forEach((count, name) => {
      if (count > maxCount) {
        maxCount = count;
        me = name;
      }
    });

    threads.sort((a, b) => b.lastTs - a.lastTs);

    return {
      threads,
      messages: allMessages,
      me,
      mediaMap,
    };
  }

  private static convertRawMessage(
    raw: RawMessage,
    threadId: number,
    id: number,
    mediaMap: Record<string, string>
  ): Msg {
    const sender = fixEncoding(raw.sender_name || 'Unknown');
    const ts = raw.timestamp_ms || Date.now();
    const content = raw.content ? fixEncoding(raw.content) : null;
    const unsent = Boolean(raw.is_unsent);

    const mediaList: MediaItem[] = [];

    const resolveMediaBlob = (uri: string): string | undefined => {
      if (mediaMap[uri]) return mediaMap[uri];
      const normalized = uri.replace(/^.*?(your_facebook_activity\/|messages\/)/i, '$1');
      if (mediaMap[normalized]) return mediaMap[normalized];
      const parts = uri.split('/');
      if (parts.length >= 2 && mediaMap[parts.slice(-2).join('/')]) {
        return mediaMap[parts.slice(-2).join('/')];
      }
      return undefined;
    };

    if (raw.photos && Array.isArray(raw.photos)) {
      for (const p of raw.photos) {
        if (p?.uri) {
          mediaList.push({
            type: 'photo',
            uri: p.uri,
            blobUrl: resolveMediaBlob(p.uri),
          });
        }
      }
    }

    if (raw.gifs && Array.isArray(raw.gifs)) {
      for (const g of raw.gifs) {
        if (g?.uri) {
          mediaList.push({
            type: 'gif',
            uri: g.uri,
            blobUrl: resolveMediaBlob(g.uri),
          });
        }
      }
    }

    if (raw.sticker?.uri) {
      mediaList.push({
        type: 'sticker',
        uri: raw.sticker.uri,
        blobUrl: resolveMediaBlob(raw.sticker.uri),
      });
    }

    if (raw.videos && Array.isArray(raw.videos)) {
      for (const v of raw.videos) {
        if (v?.uri) {
          mediaList.push({
            type: 'video',
            uri: v.uri,
            blobUrl: resolveMediaBlob(v.uri),
          });
        }
      }
    }

    if (raw.audio_files && Array.isArray(raw.audio_files)) {
      for (const a of raw.audio_files) {
        if (a?.uri) {
          mediaList.push({
            type: 'audio',
            uri: a.uri,
            blobUrl: resolveMediaBlob(a.uri),
          });
        }
      }
    }

    if (raw.files && Array.isArray(raw.files)) {
      for (const f of raw.files) {
        if (f?.uri) {
          mediaList.push({
            type: 'file',
            uri: f.uri,
            blobUrl: resolveMediaBlob(f.uri),
          });
        }
      }
    }

    let reactionsStr: string | null = null;
    if (raw.reactions && Array.isArray(raw.reactions) && raw.reactions.length > 0) {
      reactionsStr = raw.reactions
        .map((r) => `${fixEncoding(r.reaction || '')} ${fixEncoding(r.actor || '')}`.trim())
        .filter(Boolean)
        .join('\n');
    }

    const link = raw.share?.link || null;

    return {
      id,
      threadId,
      sender,
      ts,
      content,
      media: mediaList,
      link,
      reactions: reactionsStr,
      unsent,
    };
  }
}
