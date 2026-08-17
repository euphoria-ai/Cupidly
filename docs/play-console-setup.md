# Shipping Hook on Google Play

A start-to-finish runbook for putting Hook on the Play Store and keeping it there.
It assumes no prior Play Console experience. Follow it top to bottom the first
time; after that, [Every release after the first](#10-every-release-after-the-first)
is the only part you repeat.

Console menus move around. Where a path is given, treat it as a description of
what you're looking for, not a promise about the exact wording.

---

## What you are actually wiring together

Four separate systems have to agree before a single person can pay for Hook Pro.
Most setup pain is one of them not knowing about another.

| System | Owns | Hook's identifier |
|---|---|---|
| **Google Play Console** | The store listing, the app's package name, the subscription product, who can test it | `com.tom7.hook` |
| **Google Cloud** | The service account that lets RevenueCat read your Play sales data | one service account JSON |
| **RevenueCat** | The truth about who currently holds a subscription | entitlement `Hook Pro` |
| **The Hook server** | Metering the free allowance, refusing generations to non-subscribers | asks RevenueCat over its secret key |

The app itself has **no accounts and no login**. A subscription belongs to the
buyer's *Google Play account*, and Hook re-discovers it by asking Play at launch.
That single fact drives a lot of what follows.

---

## 1. Before you touch the console

### 1.1 The developer account

Go to [play.google.com/console](https://play.google.com/console) and register.

- **One-time $25 fee.** Not a subscription.
- **Identity verification is mandatory.** As an individual you upload a
  government ID and confirm an address. As an organisation you need a D-U-N-S
  number, which is free but can take up to 30 business days to be issued. If you
  intend to publish as a company, **start the D-U-N-S request now** — it is the
  longest lead time in this entire document.
- Choose individual vs organisation carefully. Switching later means a new
  account and re-publishing the app from scratch.

### 1.2 The 12-tester rule — read this before planning a launch date

If your developer account is a **personal** account created after
**13 November 2023**, Google will not let you publish to production until:

- at least **12 testers** have been opted in to a closed test, **and**
- they have stayed opted in **continuously for 14 days**, **and**
- you then apply for production access and are approved.

Internal testing does **not** count toward this. Only a *closed* test does.

This means the realistic path is: internal testing (for yourself, immediately) →
closed testing with 12+ real people (14 days minimum) → apply for production.
Budget three weeks, not three days. Organisation accounts are exempt.

### 1.3 Things you must have before you can finish the listing

- A **privacy policy at a public URL**. Hook has one at
  `https://hook.tomlin7.com/privacy` (see [`web/app/privacy`](../web/app/privacy)).
  It must be reachable without logging in and must actually describe what Hook
  collects.
- An **app icon**, 512×512 PNG.
- A **feature graphic**, 1024×500 PNG or JPG.
- **At least 2 phone screenshots**, between 320px and 3840px on any side.
- A **short description** (80 characters) and **full description** (4000).
- A contact email address that you will actually read.

---

## 2. Creating the app — and the one decision you cannot undo

In Play Console, **All apps → Create app**.

You'll be asked for a name, default language, app-vs-game, free-vs-paid.

> **Free vs paid:** choose **Free**. Hook is free to install with an in-app
> subscription. "Paid" means charging for the download itself, and **this cannot
> be changed from paid to free later**.

### The package name is permanent

The package name (Play calls it the *application ID*) is set by the first bundle
you upload and **can never be changed for that app entry**. Not renamed, not
reused — if you want a different one you create a brand new app listing and lose
all reviews, installs and testing history.

Hook currently builds as:

```
applicationId = "com.tom7.hook"
```

in [`app/app/build.gradle.kts`](../app/app/build.gradle.kts).

The marketing site's Play Store link in [`web/app/site.ts`](../web/app/site.ts)
now points at this same package. It used to say `app.hook`, which was a dead
link — the build is the source of truth, because Play binds the package name to
the app entry and the console can't be changed.

**If `com.tom7.hook` is not the name you want on the store forever, change it
now.** Internal testing has already bound that console entry to it, so a
different name means creating a fresh app entry — and updating `applicationId`
and `site.ts` together. After public release the decision is final.

---

## 3. Signing: two different keys, and why you'll never see one of them

This trips up everyone once.

- **Upload key** — yours. It lives in a `.jks` file on your machine. You sign
  every bundle with it. Play uses it only to check the upload came from you.
- **App signing key** — Google's. Play strips your upload signature and re-signs
  the app with this key before delivering it to phones. You never handle it.

This is **Play App Signing**, and it's mandatory for new apps. The upside: if you
lose your upload key, Google can reset it. If they held no signing key and you
lost yours, the app would be permanently unpublishable.

### 3.1 Create your upload keystore

Run this once. `keytool` ships with the JDK, which Android Studio installs.

```bash
keytool -genkeypair -v -keystore hook-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias hook-upload
```

It asks for a keystore password, your name/organisation, and a key password.

**Back up `hook-upload.jks` and both passwords somewhere you will still have them
in five years.** A password manager, not a folder on one laptop.

### 3.2 Point the build at it

Create `app/local.properties` if it doesn't exist. This file is gitignored and
must stay that way — it holds secrets.

```properties
RELEASE_STORE_FILE=hook-upload.jks
RELEASE_STORE_PASSWORD=your-keystore-password
RELEASE_KEY_ALIAS=hook-upload
RELEASE_KEY_PASSWORD=your-key-password
```

`RELEASE_STORE_FILE` is resolved relative to the `app/` directory, so a bare
filename means "the `.jks` sitting next to `local.properties`".

If any of these four are missing, the build still succeeds but produces an
**unsigned** release, which Play will reject. That's deliberate — a fresh clone
of the repo has to build without your secrets.

---

## 4. RevenueCat, and connecting it to Play

RevenueCat sits between the app and Google's billing API. It is what lets the
server ask "does this person have a subscription?" without implementing Google
Play Developer API polling yourself.

### 4.1 The service account (the fiddly bit)

RevenueCat needs read access to your Play sales data. That means a Google Cloud
service account.

1. Play Console → **Setup → API access**.
2. If prompted, link a Google Cloud project (create a new one — it's free).
3. **Create new service account** → this bounces you to Google Cloud Console.
4. In Cloud Console: **Create service account**, give it a name, create it, then
   open it → **Keys → Add key → Create new key → JSON**. A `.json` file
   downloads. This is a credential — treat it like a password.
5. Back in Play Console → **API access**, the new account appears. Grant it
   access and give it these permissions:
   - View financial data, orders, and cancellation survey responses
   - View app information and download bulk reports
   - Manage orders and subscriptions
6. **Wait.** Google's permission propagation can take up to 36 hours. RevenueCat
   will report the credentials as invalid until it completes. This is normal and
   there is nothing to fix — just retry the next day.

### 4.2 The RevenueCat project

1. Create a RevenueCat account and a project.
2. Add an app → **Google Play**. Enter package name `com.tom7.hook`.
3. Upload the service account JSON from step 4.1.
4. **Project settings → API keys.** You'll see two kinds of *public* key:
   - `goog_…` — talks to real Google Play Billing. **This is what ships.**
   - `test_…` — talks to RevenueCat's Test Store, a sandbox needing no Play
     account. Debug builds only.

   There is also a **secret key** (`sk_…`). It never goes in the app. It is a
   server credential and putting it in the APK would let anyone grant themselves
   Pro.

### 4.3 Wire the keys into the build

In `app/local.properties`:

```properties
REVENUECAT_PUBLIC_SDK_KEY=goog_your_production_key
REVENUECAT_TEST_SDK_KEY=test_your_sandbox_key
APP_API_KEY=the-shared-key-the-Hook-server-expects
```

- Release builds always use `REVENUECAT_PUBLIC_SDK_KEY`.
- Debug builds use `REVENUECAT_TEST_SDK_KEY` when set, falling back to the
  production key when not.

**Why they're separate:** the RevenueCat SDK refuses to run a `test_` key in a
non-debuggable build — it shows a "Wrong API Key" dialog and closes the app.
Shipping a bundle built with a test key means every tester's app dies on launch.
The build now blocks this: `bundleRelease` fails with an explanatory error if
`REVENUECAT_PUBLIC_SDK_KEY` starts with `test_`. Debug builds and Android Studio
sync are unaffected.

---

## 5. The subscription product

Two places, and they must match exactly.

### 5.1 In Play Console

**Monetise with Play → Products → Subscriptions → Create subscription.**

Modern Play subscriptions have three layers:

- **Subscription** — the thing itself, with a **product ID** (e.g. `hook_pro`).
  The product ID is permanent.
- **Base plan** — the actual billing terms: monthly, annual, price, renewal.
  A subscription can have several.
- **Offer** — optional promotions on a base plan, like a free trial or an
  introductory price.

Create the subscription, add at least one base plan, set pricing, and
**activate** both. An inactive base plan is invisible to the app and produces an
empty paywall.

### 5.2 In RevenueCat

1. **Products** → import or add the Play product you just created.
2. **Entitlements** → create one with the identifier **exactly** `Hook Pro`.
   Attach the product to it.
3. **Offerings** → create an offering, mark it **current**, and add a package
   pointing at the product.

Three names have to agree, and a mismatch is silent:

| Where | Value |
|---|---|
| RevenueCat entitlement identifier | `Hook Pro` |
| `BillingManager.ENTITLEMENT_ID` in [`BillingManager.kt`](../app/app/src/main/java/com/tomfricks/hook/billing/BillingManager.kt) | `Hook Pro` |
| Server `REVENUECAT_ENTITLEMENT_ID` | `Hook Pro` |

The app and server both fall back to "any active entitlement counts" if the
identifier doesn't match, so a typo degrades quietly rather than locking payers
out — but fix the typo rather than relying on the fallback.

The paywall reads **whatever offering is marked current**. It never assumes a
particular set of packages, so you can change pricing and plans from the
dashboard without shipping an app update.

### 5.3 Restore behaviour — do not skip this

**RevenueCat → Project settings → Restore behaviour** must be
**"Transfer to new App User ID"** (the default).

Hook has no login. It identifies each install by a random ID it generates on
first launch, which is destroyed when the app is uninstalled. When a paying
customer reinstalls, the app asks Play whether that Google account already owns
Hook Pro, and Play's answer only reattaches the subscription if this setting
permits a transfer.

Set to "Keep with original App User ID", every reinstall by a paying customer
shows them a paywall for something they already pay for, and no app-side change
can fix it.

---

## 6. The server

The server independently verifies entitlements — it never trusts the app's word
for it. Set these environment variables wherever it's deployed:

```bash
REVENUECAT_SECRET_KEY=sk_your_secret_key      # from RevenueCat, server-only
REVENUECAT_ENTITLEMENT_ID=Hook Pro            # must match the dashboard
APP_API_KEY=the-same-value-as-in-local.properties
GROQ_API_KEY=...
GEMINI_API_KEY=...                      # optional; used when Groq fails
FREE_GENERATION_LIMIT=5                       # optional, defaults to 5
```

If `REVENUECAT_SECRET_KEY` is unset the server logs a warning and treats
**everyone** as a free user, including people who have paid. It's the first thing
to check when subscribers report being blocked.

---

## 7. Building the upload

Play takes an **Android App Bundle** (`.aab`), not an APK.

```bash
cd app && ./gradlew bundleRelease -PhookVersionCode=2 -PhookVersionName=1.0.1
```

The output lands at `app/app/build/outputs/bundle/release/app-release.aab`.

**Every upload needs a `versionCode` higher than any bundle that has ever reached
the console — including ones you deleted.** It's a plain integer that only ever
goes up. `versionName` is the human-facing string and can be anything.

Pass them on the command line rather than editing the build file, so version
bumps don't produce git noise.

---

## 8. The declarations Play makes you fill in

**Policy → App content**. All of these gate publishing, and several are easy to
get wrong for an app like Hook.

### Privacy policy
The public URL from §1.3.

### Data safety
The big one, and Hook is a harder case than most apps because **it is a
keyboard**. Answer honestly:

- Hook transmits conversation text and screenshots to its own server to generate
  replies. That is collection of **user-generated content** and must be declared.
- Declare whether it's encrypted in transit (it is — HTTPS) and whether users can
  request deletion.
- Keyboards get extra scrutiny here. An inaccurate data safety form is a policy
  violation, not a paperwork slip, and is a common cause of suspension.

### Generative AI

Hook generates content with an LLM, so Google's **Generative AI policy** applies.
Its core requirement is an **in-app mechanism for users to report offensive
content** produced by the AI.

Hook has one: **long-press any generated reply in the keyboard → Report →
confirm.** The reply is replaced by an acknowledgement and the flagged text is
sent to the server. Only that one reply travels — never the screenshot, never
the surrounding conversation.

Reports arrive in the server log, one line each, prefixed `[REPORT]`:

```
[REPORT] user=<install id> reason=offensive text='the flagged reply'
```

Grep your deployment's logs for `[REPORT]`. At current volume that's enough; if
reports become frequent enough to need triage, give them a table.

When the declaration asks how users report AI content, describe the long-press
flow above.

### Photo and video permissions
Hook declares `READ_MEDIA_IMAGES` for screenshot detection. Broad photo access
requires a **declaration form** justifying why a narrower API (the photo picker)
won't do. Expect to write a paragraph explaining the screenshot flow.

### Foreground service types
Hook declares `FOREGROUND_SERVICE_DATA_SYNC`. Play requires a declaration
describing what the foreground service does and why. Have a one-paragraph
explanation of the screenshot detection service ready.

### Content rating
A questionnaire (run by IARC) that produces regional ratings automatically.
Answer honestly about romantic/suggestive themes — Hook writes flirtatious
messages, and under-rating is a policy violation. Expect Teen or higher.

### Target audience
Hook is not for children. Select adult age brackets only. Selecting any
under-13 bracket pulls you into the Families policy programme, which brings
requirements Hook does not meet.

### Ads
Hook shows no ads. Declare **no**.

### App access
Covered in detail in §9.

---

## 9. Giving Google's reviewers access

Reviewers **cannot** create accounts, use their own accounts, or make purchases.
If they can't see the paid experience, the app can be rejected.

### The answer for Hook

**Policy → App content → App access → "Is any part of your app restricted?"**

Answer **Yes**.

Hook has no login, so the instinct is to answer No — but the question is an OR,
not an AND. Its Yes list includes *"payments, such as one-time products,
memberships, subscriptions, and/or access tiers"*. Hook gates unlimited
generations and several tones behind a subscription. That's a yes.

Fill the form like this:

- **Name:** `Hook Pro reviewer access`
- **Username / email / phone:** leave blank — there is no account system
- **Password:** leave blank
- **Any other information required to access your app:**

```
Hook requires no account, login, or sign-in. Install and open the app; all
features are immediately available. Free tier = 5 AI reply generations, then
a paywall.

To review Hook Pro (unlimited generations, all Pro tones and reply lengths)
at no cost:
1. Play Store app > Payments & subscriptions > Redeem code
2. Redeem one code below
3. Open Hook > Settings > "Restore purchases"

CODE-1
CODE-2
CODE-3

Codes are single use; several are provided.
```

Then tick the box confirming the details give full access to premium content.

### Generating those codes

**Monetise with Play → Promotions → Create promotion**, targeting the Hook Pro
subscription. Generate a batch of one-time codes and paste 3–5 into the field.

Two caveats:

- Codes are **single-use and expire**. Google asks for reusable, non-expiring
  credentials, which promo codes aren't. Supplying several is the accepted
  workaround, but **regenerate them and update this declaration before every
  submission** — a used-up batch is a rejection.
- **License testing does not help here.** It works from an email allowlist and
  you don't know the reviewer's account.

---

## 10. Testing before anyone real sees it

### 10.1 License testers — free purchases for your own accounts

**Setup → License testing.** Add the Google account emails of anyone who needs to
test buying. Those accounts see real purchase dialogs marked as test purchases,
are never charged, and get accelerated subscription renewals (a month passes in
minutes) so you can watch renewal and expiry.

This is the correct way to test billing. It requires the `goog_` production key,
not the `test_` one.

### 10.2 The tracks

| Track | Who | Notes |
|---|---|---|
| **Internal** | Up to 100 named testers | Fastest. Where you catch launch-blocking bugs. Does **not** count toward the 12-tester rule. |
| **Closed** | An opt-in list or Google Group | **This is the one the 12-tester rule needs.** |
| **Open** | Anyone with the link | Public beta. |
| **Production** | Everyone | Needs approved production access first. |

Upload the `.aab` to **Release → Testing → Internal testing → Create new
release**, add testers, and share the opt-in link. Testers must accept the
invitation before the Play Store will show them the app.

### 10.3 What to actually verify

- The app opens without a "Wrong API Key" dialog — that means the production key
  is in the bundle.
- The paywall lists your base plans with correct prices.
- A test purchase completes and Pro unlocks.
- The keyboard stops showing the free-allowance counter after purchase.
- **Uninstall, reinstall, open.** Pro should come back on its own within a few
  seconds, without touching Restore. This is the flow §5.3 protects.
- **Restore purchases** in Settings still works and reports its outcome.

---

## 11. Every release after the first

1. Bump `versionCode`. Higher than anything ever uploaded.
2. `./gradlew bundleRelease -PhookVersionCode=N -PhookVersionName=X.Y.Z`
3. Upload to **Internal testing** first, always.
4. Verify the checklist in §10.3.
5. Promote the release to the next track rather than re-uploading.
6. If reviewer promo codes were used, generate fresh ones and update the App
   access declaration.

Review takes anywhere from a few hours to about a week. First submissions are
slowest.

---

## 12. When something goes wrong

**"Wrong API Key" dialog, app closes on launch**
A `test_` RevenueCat key in a release build. Put the `goog_` key in
`REVENUECAT_PUBLIC_SDK_KEY`, rebuild with a higher `versionCode`, re-upload. The
build now refuses to produce a release bundle in this state.

**Paywall is empty, or shows the fallback screen**
No offering is marked *current* in RevenueCat, or the base plan isn't activated
in Play Console, or the product IDs don't match. Check all three.

**"You already own this item"**
A previous purchase Play still knows about. Tap **Restore purchases**. This is
also what a reinstalling customer sees if they tap Subscribe instead of waiting.

**Subscriber gets 402 / "out of free generations"**
Server-side. Check `REVENUECAT_SECRET_KEY` is set in the deployment, and that
`REVENUECAT_ENTITLEMENT_ID` matches the dashboard. The server caches entitlement
answers for 60 seconds, so a purchase can take up to a minute to take effect if
the post-purchase refresh call failed.

**Reinstall doesn't restore Pro automatically**
Check **Restore behaviour** is "Transfer to new App User ID" (§5.3). Then check
the device has connectivity at launch — a failed attempt is retried on the next
launch rather than silently giving up.

**RevenueCat says the service account credentials are invalid**
Wait. Google's permission propagation takes up to 36 hours after granting access.

**Upload rejected: "versionCode already used"**
Bump it. Deleted releases still burn their version code permanently.

---

## Known gaps

Things this setup deliberately doesn't do, so nobody discovers them by surprise:

- **Pro follows the install, not the person.** With no login, one Play account's
  subscription can only be active on one install at a time. A second device needs
  a manual **Restore purchases**, and doing so moves Pro off the first device.
- **The free allowance resets on reinstall.** The install ID is regenerated, so
  the server sees a new user with a fresh 5 generations. Fixing it requires
  either real accounts or device-level identification.
- **Reviewer promo codes expire.** There is no non-expiring credential for a
  no-login paid app. This needs manual attention at each submission.
- **Content reports go to the log, not a table.** Fine at low volume, and it
  means reports are subject to whatever log retention your host gives you.

## Launch blockers, in order

The things that will stop a production release, longest lead time first:

1. **D-U-N-S number**, if publishing as an organisation — up to 30 business days.
2. **12 testers for 14 continuous days** in a closed test, for personal accounts
   created after 13 Nov 2023.
3. **Confirm the package name** — `com.tom7.hook` is permanent once public.
4. **Service account propagation** — up to 36 hours, one time.

In-app content reporting for the Generative AI policy was previously on this
list. It is built — see §8.
