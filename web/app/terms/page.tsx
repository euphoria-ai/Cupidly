import type { Metadata } from "next";
import { LegalPage, Section } from "../legal";
import { site } from "../site";

export const metadata: Metadata = { title: "Terms of Service" };

export default function Terms() {
  return (
    <LegalPage title="Terms of Service">
      <p>
        These terms are an agreement between you and {site.name} covering the
        {" "}{site.name} Android app and this website. By using them, you accept
        these terms.
      </p>

      <Section heading="Eligibility">
        <p>
          You must be at least 13 years old, or the minimum age of digital
          consent in your country, whichever is higher. If you are under 18, you
          need permission from a parent or guardian.
        </p>
      </Section>

      <Section heading="Your license">
        <p>
          We grant you a personal, non-exclusive, non-transferable, revocable
          license to use {site.name} for your own use. You may not copy, resell,
          reverse engineer, or build a competing service from the app, and you
          may not use it to break the law or the rules of any platform you use it
          on.
        </p>
      </Section>

      <Section heading="AI-generated content">
        <p>
          Suggestions are produced by an AI model and may be inaccurate,
          inappropriate, or unoriginal. You decide what to send, and you are
          responsible for what you send. Do not use {site.name} to harass,
          deceive, impersonate, spam, or generate sexual content involving
          minors.
        </p>
      </Section>

      <Section heading="Subscriptions and billing">
        <p>
          Paid plans are sold through Google Play. Subscriptions renew
          automatically at the interval shown at purchase until cancelled.
          Cancel or manage renewal in the Google Play subscriptions settings at
          least 24 hours before the period ends. Refunds are handled under
          Google Play&rsquo;s refund policy.
        </p>
      </Section>

      <Section heading="Termination">
        <p>
          You may stop using {site.name} and delete your account at any time. We
          may suspend or terminate access if you breach these terms or use the
          service in a way that harms others or us.
        </p>
      </Section>

      <Section heading="Disclaimers and liability">
        <p>
          The service is provided &ldquo;as is&rdquo; without warranties of any kind, to
          the extent permitted by law. We are not liable for indirect or
          consequential damages, and our total liability is limited to the amount
          you paid us in the twelve months before the claim. Nothing here limits
          rights that cannot be limited under applicable consumer law.
        </p>
      </Section>

      <Section heading="Changes">
        <p>
          We may update these terms. Continued use after an update means you
          accept the revised terms.
        </p>
      </Section>

      <Section heading="Contact">
        <p>
          Questions:{" "}
          <a
            className="underline underline-offset-4"
            href={`mailto:${site.supportEmail}`}
          >
            {site.supportEmail}
          </a>
          .
        </p>
      </Section>
    </LegalPage>
  );
}
