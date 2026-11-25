import { Dialog, DialogBackdrop, DialogPanel } from "@headlessui/react";
import type { ReactNode } from "react";
import { IoIosClose } from "react-icons/io";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}

export default function Modal({ open, onClose, children }: ModalProps) {
  return (
    <Dialog open={open} as="div" className="relative z-40 text-white focus:outline-none" onClose={onClose}>
      <DialogBackdrop className="fixed inset-0 bg-black/65" />
      <div className="fixed inset-0 z-40 w-screen overflow-y-auto p-4">
        <div className="flex min-h-full items-center justify-center">
          <DialogPanel
            transition
            className="relative w-full max-w-md rounded-2xl border border-white/10 bg-sleek_gray p-6 shadow-2xl duration-200 ease-out data-[closed]:scale-95 data-[closed]:opacity-0"
          >
            <button
              aria-label="Close"
              className="absolute right-3 top-3 rounded-full p-1 text-gray-400 transition hover:bg-white/10 hover:text-white"
              onClick={onClose}
            >
              <IoIosClose size={26} />
            </button>
            {children}
          </DialogPanel>
        </div>
      </div>
    </Dialog>
  );
}
