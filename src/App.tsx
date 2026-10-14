import { useEffect, useState, useCallback } from 'react';
import { ThreadRow, Screen, AppPrefs } from './types';
import { dbStorage } from './db/storage';
import { SetupScreen } from './components/SetupScreen';
import { HomeScreen } from './components/HomeScreen';
import { ChatScreen } from './components/ChatScreen';
import { SearchScreen } from './components/SearchScreen';

export function App() {
  const [prefs, setPrefs] = useState<AppPrefs>({ imported: false, me: '' });
  const [threads, setThreads] = useState<ThreadRow[]>([]);
  const [showSetup, setShowSetup] = useState(false);
  const [screen, setScreen] = useState<Screen>({ type: 'home' });
  const [loadingInitial, setLoadingInitial] = useState(true);

  // Load state from local database
  const refreshData = useCallback(async () => {
    try {
      const p = await dbStorage.getPrefs();
      setPrefs(p);
      if (p.imported) {
        const t = await dbStorage.getThreads();
        setThreads(t);
        setShowSetup(false);
      } else {
        setShowSetup(true);
      }
    } finally {
      setLoadingInitial(false);
    }
  }, []);

  useEffect(() => {
    refreshData();
  }, [refreshData]);

  // Handle browser back button (like Android BackHandler)
  useEffect(() => {
    const handlePopState = () => {
      setScreen((prev) => {
        if (prev.type === 'search' && prev.thread) {
          return { type: 'chat', thread: prev.thread, focusId: null };
        }
        return { type: 'home' };
      });
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateTo = (nextScreen: Screen) => {
    window.history.pushState({}, '');
    setScreen(nextScreen);
  };

  const handleBack = () => {
    if (screen.type === 'search' && screen.thread) {
      setScreen({ type: 'chat', thread: screen.thread, focusId: null });
    } else {
      setScreen({ type: 'home' });
    }
  };

  if (loadingInitial) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-950 text-slate-400 text-sm">
        Loading Chat Viewer...
      </div>
    );
  }

  // Need setup screen if not imported yet or user clicked "Re-import"
  const needSetup = !prefs.imported || showSetup;

  if (needSetup) {
    return (
      <SetupScreen
        canCancel={prefs.imported}
        onCancel={() => setShowSetup(false)}
        onImportComplete={async () => {
          await refreshData();
          setShowSetup(false);
          setScreen({ type: 'home' });
        }}
      />
    );
  }

  switch (screen.type) {
    case 'home':
      return (
        <HomeScreen
          threads={threads}
          onOpen={(thread) => navigateTo({ type: 'chat', thread, focusId: null })}
          onSearch={() => navigateTo({ type: 'search', thread: null })}
          onReimport={() => setShowSetup(true)}
        />
      );

    case 'chat':
      return (
        <ChatScreen
          thread={screen.thread}
          focusId={screen.focusId}
          me={prefs.me}
          onBack={handleBack}
          onSearch={() => navigateTo({ type: 'search', thread: screen.thread })}
        />
      );

    case 'search':
      return (
        <SearchScreen
          scope={screen.thread}
          onBack={handleBack}
          onOpen={(hit) => {
            const t = threads.find((item) => item.id === hit.threadId);
            if (t) {
              setScreen({ type: 'chat', thread: t, focusId: hit.msgId });
            }
          }}
        />
      );

    default:
      return null;
  }
}

export default App;
