import React, { useRef, useState } from 'react';
import { FolderUp, FileArchive, Sparkles, AlertCircle, ShieldCheck, RefreshCw, X } from 'lucide-react';
import { ChatImporter } from '../utils/importer';
import { dbStorage } from '../db/storage';
import { getSampleChatData } from '../utils/sampleData';

interface SetupScreenProps {
  canCancel: boolean;
  onCancel: () => void;
  onImportComplete: () => void;
}

export const SetupScreen: React.FC<SetupScreenProps> = ({
  canCancel,
  onCancel,
  onImportComplete,
}) => {
  const [isRunning, setIsRunning] = useState(false);
  const [status, setStatus] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);

  const folderInputRef = useRef<HTMLInputElement>(null);
  const zipInputRef = useRef<HTMLInputElement>(null);

  const handleFolderSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    await startImportFromFiles(files);
  };

  const handleZipSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    const file = files[0];
    if (file) {
      await startImportFromZip(file);
    }
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    if (isRunning) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const first = e.dataTransfer.files[0];
      if (first.name.endsWith('.zip')) {
        await startImportFromZip(first);
      } else {
        await startImportFromFiles(e.dataTransfer.files);
      }
    }
  };

  const startImportFromFiles = async (files: FileList | File[]) => {
    try {
      setIsRunning(true);
      setError(null);
      const data = await ChatImporter.importFromFiles(files, (msg) => {
        setStatus(msg);
      });

      setStatus('Saving chats to local offline database...');
      await dbStorage.saveImportData(data.threads, data.messages, data.me, data.mediaMap);
      setStatus(`Done: ${data.threads.length} chats imported`);
      onImportComplete();
    } catch (err: unknown) {
      console.error(err);
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsRunning(false);
    }
  };

  const startImportFromZip = async (zipFile: File) => {
    try {
      setIsRunning(true);
      setError(null);
      const data = await ChatImporter.importFromZip(zipFile, (msg) => {
        setStatus(msg);
      });

      setStatus('Saving chats to local offline database...');
      await dbStorage.saveImportData(data.threads, data.messages, data.me, data.mediaMap);
      setStatus(`Done: ${data.threads.length} chats imported`);
      onImportComplete();
    } catch (err: unknown) {
      console.error(err);
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsRunning(false);
    }
  };

  const loadSample = async () => {
    try {
      setIsRunning(true);
      setError(null);
      setStatus('Loading sample Facebook Messenger export...');
      const sample = getSampleChatData();
      await dbStorage.saveImportData(sample.threads, sample.messages, sample.me);
      setStatus('Done loading sample chats!');
      onImportComplete();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col items-center justify-center p-4 sm:p-6 text-slate-800 dark:text-slate-100">
      <div className="w-full max-w-xl bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200/80 dark:border-slate-800 p-6 sm:p-8">
        
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-blue-600 text-white flex items-center justify-center font-bold text-xl shadow-md shadow-blue-500/20">
              💬
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight">Chat Viewer</h1>
              <p className="text-xs text-slate-500 dark:text-slate-400">Meta Messenger Archive Explorer</p>
            </div>
          </div>
          {canCancel && !isRunning && (
            <button
              onClick={onCancel}
              className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg transition"
              title="Close and return to chats"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {isRunning ? (
          <div className="py-8 flex flex-col items-center text-center">
            <RefreshCw className="w-10 h-10 text-blue-600 animate-spin mb-4" />
            <h3 className="text-lg font-semibold mb-2">Importing your messages</h3>
            <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md mb-6">
              Please keep this tab open; processing large archives may take a moment.
            </p>
            <div className="w-full bg-slate-100 dark:bg-slate-800 rounded-xl p-4 border border-slate-200 dark:border-slate-700/60 font-mono text-xs whitespace-pre-line text-slate-700 dark:text-slate-300 min-h-[72px] flex items-center justify-center text-center">
              {status || 'Preparing import...'}
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            <div className="space-y-3 text-sm text-slate-600 dark:text-slate-300">
              <div className="flex items-start gap-3 bg-slate-100/70 dark:bg-slate-800/60 p-3.5 rounded-xl border border-slate-200/60 dark:border-slate-800">
                <span className="font-semibold text-blue-600 dark:text-blue-400 shrink-0">Step 1:</span>
                <span>Download your archive from Meta (Accounts Center → Your information and permissions → Export your information → Messages).</span>
              </div>
              <div className="flex items-start gap-3 bg-slate-100/70 dark:bg-slate-800/60 p-3.5 rounded-xl border border-slate-200/60 dark:border-slate-800">
                <span className="font-semibold text-blue-600 dark:text-blue-400 shrink-0">Step 2:</span>
                <span>Upload either the extracted folder (containing <code className="text-xs bg-slate-200 dark:bg-slate-700 px-1 py-0.5 rounded">your_facebook_activity</code>) or directly select the downloaded ZIP file.</span>
              </div>
            </div>

            {/* Drag & Drop Zone */}
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setDragActive(true);
              }}
              onDragLeave={() => setDragActive(false)}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-xl p-6 text-center transition-all ${
                dragActive
                  ? 'border-blue-500 bg-blue-50/50 dark:bg-blue-950/20'
                  : 'border-slate-300 dark:border-slate-700 hover:border-blue-400 bg-slate-50/50 dark:bg-slate-800/30'
              }`}
            >
              <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={() => folderInputRef.current?.click()}
                  className="w-full sm:w-auto px-5 py-2.5 bg-blue-600 hover:bg-blue-700 active:scale-[0.98] text-white font-medium rounded-xl text-sm transition shadow-sm flex items-center justify-center gap-2"
                >
                  <FolderUp className="w-4 h-4" />
                  Choose export folder
                </button>
                <span className="text-xs text-slate-400 font-medium">or</span>
                <button
                  type="button"
                  onClick={() => zipInputRef.current?.click()}
                  className="w-full sm:w-auto px-4 py-2.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-medium rounded-xl text-sm transition flex items-center justify-center gap-2"
                >
                  <FileArchive className="w-4 h-4" />
                  Select .ZIP file
                </button>
              </div>

              {/* Hidden file inputs */}
              <input
                ref={folderInputRef}
                type="file"
                // @ts-expect-error webkitdirectory is standard in browsers but non-standard TS
                webkitdirectory=""
                directory=""
                multiple
                className="hidden"
                onChange={handleFolderSelect}
              />
              <input
                ref={zipInputRef}
                type="file"
                accept=".zip,application/zip"
                className="hidden"
                onChange={handleZipSelect}
              />

              <p className="mt-3 text-xs text-slate-400">
                Or drag and drop your export folder or zip archive here
              </p>
            </div>

            {/* Quick Demo Option */}
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-200 dark:border-slate-800">
              <button
                type="button"
                onClick={loadSample}
                className="text-xs font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400 flex items-center gap-1.5 py-1 px-2 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-950/40 transition"
              >
                <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                Don&apos;t have an archive yet? Load sample chats demo
              </button>
              {canCancel && (
                <button
                  type="button"
                  onClick={onCancel}
                  className="text-xs text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 font-medium"
                >
                  Cancel
                </button>
              )}
            </div>

            {/* Privacy notice */}
            <div className="flex items-center gap-2 text-xs text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 p-2.5 rounded-lg border border-emerald-200/50 dark:border-emerald-900/50">
              <ShieldCheck className="w-4 h-4 shrink-0" />
              <span>Your messages are parsed entirely in your browser. No files are uploaded to any server.</span>
            </div>

            {/* Error or Status */}
            {error && (
              <div className="flex items-start gap-2.5 text-xs text-rose-700 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/30 p-3 rounded-lg border border-rose-200 dark:border-rose-900">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="whitespace-pre-line">{error}</span>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
