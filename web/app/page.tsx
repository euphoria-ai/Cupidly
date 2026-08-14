import Image from "next/image";
import HookMark from "./hook-mark";
import { site } from "./site";

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center px-6 py-24">
      <div className="flex flex-col items-center gap-6 sm:flex-row sm:gap-7">
        <HookMark size={96} />
        <div className="flex flex-col items-center sm:items-start">
          <h1 className="text-5xl font-semibold tracking-tight sm:text-6xl">
            {site.name}
          </h1>
          <p className="mt-1.5 text-sm text-zinc-500 dark:text-zinc-400">
            {site.tagline}
          </p>
        </div>
      </div>

      <a
        href={site.playStoreUrl}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Get ${site.name} on Google Play`}
        className="mt-12 rounded-[7px] shadow-card transition-[box-shadow,transform] duration-300 ease-out hover:-translate-y-0.5 hover:shadow-card-hover focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-zinc-500"
      >
        <Image
          src="/google-play-badge.svg"
          alt="Get it on Google Play"
          width={162}
          height={48}
          fetchPriority="high"
          className="block"
        />
      </a>
    </main>
  );
}
