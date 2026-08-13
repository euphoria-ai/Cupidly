import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Footer from "./footer";
import { site } from "./site";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  metadataBase: new URL(site.url),
  title: {
    default: `${site.name} — ${site.tagline}`,
    template: `%s — ${site.name}`,
  },
  description:
    "Hook is the AI keyboard that writes your replies for you. Available on Google Play.",
  openGraph: {
    title: `${site.name} — ${site.tagline}`,
    description:
      "Hook is the AI keyboard that writes your replies for you. Available on Google Play.",
    url: site.url,
    siteName: site.name,
    type: "website",
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-[radial-gradient(120%_120%_at_50%_0%,#ffffff_0%,#eef0f3_55%,#d8dce2_100%)] font-sans text-zinc-900 dark:bg-[radial-gradient(120%_120%_at_50%_0%,#141414_0%,#0b0b0b_60%,#000000_100%)] dark:text-zinc-100">
        <div className="flex flex-1 flex-col">{children}</div>
        <Footer />
      </body>
    </html>
  );
}
