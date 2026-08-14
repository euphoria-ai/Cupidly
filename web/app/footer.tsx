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
            href={site.xUrl}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Hook on X"
            className={linkClass}
          >
            {/* Padded viewBox: the solid X fills more of its box than the
                outlined Instagram glyph, so inset it to match optically. */}
            <svg
              viewBox="-2 -2 28 28"
              width="16"
              height="16"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M18.24 2.25h3.31l-7.23 8.26 8.5 11.24h-6.66l-5.21-6.82-5.96 6.82H1.68l7.73-8.84L1.25 2.25h6.83l4.71 6.23zm-1.16 17.52h1.83L7.08 4.13H5.11z" />
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
