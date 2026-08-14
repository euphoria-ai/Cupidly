import type { Metadata } from "next";
import { LegalPage, Section } from "../legal";
import { site } from "../site";

export const metadata: Metadata = { title: "Contact" };

export default function Contact() {
  return (
    <LegalPage title="Contact">
      <p>
        Need help, found a bug, or want to delete your account? Email us and we
        reply within two business days.
      </p>

      <Section heading="Support">
        <p>
          <a
            className="underline underline-offset-4"
            href={`mailto:${site.supportEmail}`}
          >
            {site.supportEmail}
          </a>
        </p>
        <p>
          Include your device model, Android version, and app version so we can
          reproduce the issue faster.
        </p>
      </Section>

      <Section heading="Billing and refunds">
        <p>
          Subscriptions are billed by Google Play. Manage or cancel a plan in the
          Play Store under Payments &amp; subscriptions. Refund requests go
          through Google Play, but email us and we will help.
        </p>
      </Section>

      <Section heading="Account deletion">
        <p>
          Email us from the address on your account with the subject &ldquo;Delete my
          account&rdquo; and we will erase your data.
        </p>
      </Section>

      <Section heading="Social">
        <p className="flex gap-4">
          <a
            className="underline underline-offset-4"
            href={site.xUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            X
          </a>
          <a
            className="underline underline-offset-4"
            href={site.instagramUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            Instagram
          </a>
        </p>
      </Section>
    </LegalPage>
  );
}
