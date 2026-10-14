export interface MediaItem {
  type: 'photo' | 'video' | 'audio' | 'gif' | 'sticker' | 'file';
  uri: string;
  blobUrl?: string;
  name?: string;
}

export interface ReactionItem {
  reaction: string;
  actor: string;
}

export interface Msg {
  id: number;
  threadId: number;
  sender: string;
  ts: number; // epoch ms
  content: string | null;
  media: MediaItem[];
  link: string | null;
  reactions: string | null; // newline-separated "emoji actor" or formatted
  unsent: boolean;
}

export interface ThreadRow {
  id: number;
  title: string;
  folder: string;
  people: number;
  count: number;
  lastTs: number;
  preview: string;
  participants: string[];
}

export interface Hit {
  msgId: number;
  threadId: number;
  title: string;
  sender: string;
  ts: number;
  content: string;
}

export interface AppPrefs {
  imported: boolean;
  me: string;
  treeName?: string;
  lastImportTs?: number;
}

export type Screen =
  | { type: 'home' }
  | { type: 'chat'; thread: ThreadRow; focusId: number | null }
  | { type: 'search'; thread: ThreadRow | null };
