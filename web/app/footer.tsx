import Link from "next/link";
import { site } from "./site";

const linkClass =
  "text-zinc-400 transition-colors hover:text-zinc-600 dark:text-zinc-600 dark:hover:text-zinc-400";

export default function Footer() {
  return (
    <footer className="w-full px-6 pb-8 pt-16">
      <div className="mx-auto flex w-full max-w-3xl flex-col items-center justify-between gap-4 text-sm sm:flex-row">
        <nav className="flex items-center gap-5">
          <Link href="/contact" className={linkClass}>
            Support
          </Link>
          <Link href="/privacy" className={linkClass}>
            Privacy
          </Link>
          <Link href="/terms" className={linkClass}>
            Terms
          </Link>
        </nav>
        <div className="flex items-center gap-4">
          <a
            href={site.tiktokUrl}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Hook on TikTok"
            className={linkClass}
          >
            <svg
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M16.6 5.82A4.28 4.28 0 0 1 15.54 3h-3.09v12.4a2.59 2.59 0 0 1-2.59 2.5 2.59 2.59 0 1 1 .77-5.06V9.7a5.67 5.67 0 0 0-.77-.05A5.65 5.65 0 1 0 15.54 15.3V8.99a7.34 7.34 0 0 0 4.28 1.37V7.27a4.28 4.28 0 0 1-3.22-1.45z" />
            </svg>
          </a>
          <a
            href={site.instagramUrl}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Hook on Instagram"
            className={linkClass}
          >
            <svg
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              aria-hidden="true"
            >
              <rect x="3" y="3" width="18" height="18" rx="5" />
              <circle cx="12" cy="12" r="4" />
              <circle cx="17.2" cy="6.8" r="1" fill="currentColor" stroke="none" />
            </svg>
          </a>
        </div>
      </div>
    </footer>
  );
}
