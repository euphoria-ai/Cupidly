import Link from "next/link";
import { site } from "./site";

export function LegalPage({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-20">
      <Link
        href="/"
        className="text-sm text-zinc-400 transition-colors hover:text-zinc-600 dark:text-zinc-600 dark:hover:text-zinc-400"
      >
        ← {site.name}
      </Link>
      <h1 className="mt-8 text-3xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">
        Last updated: {site.lastUpdated}
      </p>
      <div className="mt-10 flex flex-col gap-8 text-[15px] leading-7 text-zinc-700 dark:text-zinc-300">
        {children}
      </div>
    </main>
  );
}

export function Section({
  heading,
  children,
}: {
  heading: string;
  children: React.ReactNode;
}) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
        {heading}
      </h2>
      {children}
    </section>
  );
}
