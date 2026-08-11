import Image from "next/image";
import { site } from "./site";

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center px-6 py-24">
      <div className="flex items-center gap-5">
        <Image
          src="/app-icon.svg"
          alt={`${site.name} app icon`}
          width={76}
          height={76}
          priority
          className="h-[76px] w-[76px] rounded-[18px] shadow-[0_6px_20px_rgba(0,0,0,0.12)]"
        />
        <div className="flex flex-col">
          <h1 className="text-5xl font-semibold tracking-tight">{site.name}</h1>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            {site.tagline}
          </p>
        </div>
      </div>

      <a
        href={site.playStoreUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="mt-12 transition-opacity hover:opacity-80"
      >
        <Image
          src="/google-play-badge.svg"
          alt="Get it on Google Play"
          width={162}
          height={48}
          priority
        />
      </a>
    </main>
  );
}
