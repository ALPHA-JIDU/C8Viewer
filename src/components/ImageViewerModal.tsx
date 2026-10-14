import React, { useEffect } from 'react';
import { X, ZoomIn } from 'lucide-react';

interface ImageViewerModalProps {
  src: string;
  onClose: () => void;
}

export const ImageViewerModal: React.FC<ImageViewerModalProps> = ({ src, onClose }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 bg-black/95 backdrop-blur-xs flex items-center justify-center p-4 cursor-pointer select-none animate-in fade-in duration-150"
    >
      <button
        onClick={onClose}
        className="absolute top-4 right-4 p-2.5 bg-white/10 hover:bg-white/20 text-white rounded-full transition"
        title="Close (Esc)"
      >
        <X className="w-5 h-5" />
      </button>

      <div
        onClick={(e) => e.stopPropagation()}
        className="max-w-4xl max-h-[90vh] flex flex-col items-center cursor-default"
      >
        <img
          src={src}
          alt="Chat attachment"
          className="max-w-full max-h-[85vh] object-contain rounded-lg shadow-2xl"
        />
        <div className="mt-3 flex items-center gap-2 text-xs text-white/70">
          <ZoomIn className="w-3.5 h-3.5" />
          <span>Click anywhere outside or press Esc to close</span>
        </div>
      </div>
    </div>
  );
};
