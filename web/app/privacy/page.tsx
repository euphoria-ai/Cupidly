import type { Metadata } from "next";
import { LegalPage, Section } from "../legal";
import { site } from "../site";

export const metadata: Metadata = { title: "Privacy Policy" };

export default function Privacy() {
  return (
    <LegalPage title="Privacy Policy">
      <p>
        This policy explains what {site.name} (&ldquo;we&rdquo;, &ldquo;us&rdquo;) collects when you
        use the {site.name} Android app and this website, how we use it, and the
        choices you have.
      </p>

      <Section heading="Information we collect">
        <p>
          <strong>Text you send to the AI.</strong> When you ask {site.name} to
          generate or rewrite a reply, the text you select or type for that
          request is sent to our servers and to our AI provider so a suggestion
          can be produced.
        </p>
        <p>
          <strong>What we do not collect.</strong> {site.name} does not record or
          transmit everything you type. Keystrokes typed normally in other apps
          are not sent anywhere. We do not read your messages in the background.
        </p>
        <p>
          <strong>Account and purchase data.</strong> If you create an account or
          buy a subscription, we store an account identifier and the
          subscription status reported by Google Play. Payment card details are
          handled by Google Play and are never seen by us.
        </p>
        <p>
          <strong>Device and usage data.</strong> We collect basic diagnostics —
          app version, device model, OS version, crash reports, and anonymous
          feature-usage events — to keep the app working.
        </p>
      </Section>

      <Section heading="How we use it">
        <p>
          To generate suggestions, operate and improve the app, provide support,
          detect abuse and fraud, and meet legal obligations. We do not sell your
          personal information, and we do not use your message content for
          advertising.
        </p>
      </Section>

      <Section heading="Who we share it with">
        <p>
          Service providers who process data on our behalf: our AI model
          provider, cloud hosting, crash reporting and analytics, and Google Play
          for billing. They may only use the data to perform services for us. We
          may also disclose data if required by law.
        </p>
      </Section>

      <Section heading="Retention">
        <p>
          Request text is retained only as long as needed to produce and deliver
          a suggestion, plus a short window for abuse prevention and debugging,
          after which it is deleted or anonymized. Account records are kept while
          your account is active.
        </p>
      </Section>

      <Section heading="Your rights">
        <p>
          You can request access to, correction of, or deletion of your personal
          data, and you can delete your account at any time from the app
          settings or by emailing us. Depending on where you live, you may also
          have rights to object to or restrict processing and to data
          portability.
        </p>
      </Section>

      <Section heading="Children">
        <p>
          {site.name} is not directed to children under 13 (or the minimum age
          required in your country), and we do not knowingly collect their data.
        </p>
      </Section>

      <Section heading="Changes">
        <p>
          We may update this policy. Material changes will be announced in the
          app or on this page, and the &ldquo;last updated&rdquo; date above will change.
        </p>
      </Section>

      <Section heading="Contact">
        <p>
          Questions or requests:{" "}
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
