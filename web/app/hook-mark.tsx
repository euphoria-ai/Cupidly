import Image from "next/image";
import { site } from "./site";

/**
 * The brand tile. Two assets, one per colour scheme: the black tile carries the
 * light scheme, the white tile the dark one. Both PNGs bake in a 24%-of-width
 * corner radius, so the wrapper matches that radius to keep the drop shadow
 * hugging the silhouette instead of casting a rectangle.
 *
 * Neither image is preloaded — lazy loading means the browser only fetches the
 * one that is actually displayed. `fetchPriority` still moves it up the queue.
 */
export default function HookMark({ size = 96 }: { size?: number }) {
  const alt = `${site.name} app icon`;

  return (
    <div
      style={{ width: size, height: size }}
      className="shrink-0 overflow-hidden rounded-[24%] shadow-tile transition-[box-shadow,transform] duration-300 ease-out hover:-translate-y-0.5 hover:shadow-tile-hover"
    >
      <Image
        src="/hook-icon-light.png"
        alt={alt}
        width={size}
        height={size}
        fetchPriority="high"
        className="h-full w-full dark:hidden"
      />
      <Image
        src="/hook-icon-dark.png"
        alt={alt}
        width={size}
        height={size}
        fetchPriority="high"
        className="hidden h-full w-full dark:block"
      />
    </div>
  );
}
