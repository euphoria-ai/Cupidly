# Graph Report - Cupidly-main  (2026-08-14)

## Corpus Check
- 90 files · ~118,946 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 952 nodes · 1820 edges · 51 communities (42 shown, 9 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 44 edges (avg confidence: 0.67)
- Token cost: 75,935 input · 0 output

## Community Hubs (Navigation)
- Onboarding & Demo Screens
- Remotion Reel Components
- Rizz Session State Machine
- Keyboard IME Service
- Marketing Reel Design Rules
- Paywall & Screen Routing
- Web App Dependencies
- Android API Client
- Demo Chat Walkthrough
- RevenueCat Billing Manager
- Remotion Project Manifest
- Allowance Store Abstraction
- Web TypeScript Config
- FastAPI Route Handlers
- Prompt & Context Building
- Screenshot Detection Service
- Entitlement Gating Tests
- Marketing Site Pages
- SQLite Allowance Store
- Generation Edge-Case Tests
- Allowance & Auth Tests
- RevenueCat Entitlement Cache
- Remotion TypeScript Config
- API DTOs & Conversation Session
- Preferences Repository
- Groq Rate-Limit Backoff
- Supabase Store Test Doubles
- Onboarding Persistence Tests
- Theme & Message Style Prefs
- Onboarding Field Enum
- Onboarding Sync
- Message Tone Enum
- Emoji Use Enum
- Flirt Level Enum
- Reply Length Enum
- Server API-Key Auth
- Android Application Entry
- Gradle Wrapper Script
- Instrumented Test Sample
- Gemini Response Models
- Unit Test Sample
- Onboarding Shell Test
- ESLint Config
- Next.js Config
- PostCSS Config
- Server Package Root

## God Nodes (most connected - your core abstractions)
1. `PreferencesRepository` - 41 edges
2. `SqliteAllowanceStore` - 30 edges
3. `RizzSession` - 28 edges
4. `ApiService` - 27 edges
5. `BillingManager` - 27 edges
6. `HookKeyboardService` - 23 edges
7. `PebbleButton()` - 22 edges
8. `SupabaseAllowanceStore` - 22 edges
9. `_headers()` - 21 edges
10. `ScreenshotDetectionService` - 20 edges

## Surprising Connections (you probably didn't know these)
- `Read node_modules/next/dist/docs before coding` --semantically_similar_to--> `Kotlin/Compose UI parity rule`  [INFERRED] [semantically similar]
  web/AGENTS.md → marketing/README.md
- `CustomerCenterScreen()` --calls--> `CustomerCenter`  [INFERRED]
  app/app/src/main/java/com/tomfricks/hook/ui/screens/subscription/CustomerCenterScreen.kt → app/app/src/main/java/com/tomfricks/hook/ui/navigation/Navigation.kt
- `pytest CI job` --references--> `Server dev/test dependencies`  [INFERRED]
  .github/workflows/server-tests.yml → server/requirements-dev.txt
- `pytest CI job` --references--> `pytest + pytest-asyncio test stack`  [INFERRED]
  .github/workflows/server-tests.yml → server/requirements-dev.txt
- `Next.js web app (create-next-app)` --conceptually_related_to--> `Hook Instagram Reels Remotion project`  [INFERRED]
  web/README.md → marketing/README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Remotion reel UI rebuilt from Compose sources** — marketing_readme_theme_ts, marketing_readme_igchat, marketing_readme_hookkeyboard, marketing_readme_phone, marketing_readme_demochatstep_kt, marketing_readme_keyboardpanel_kt, marketing_readme_color_kt, marketing_readme_kotlin_ui_parity [EXTRACTED 1.00]
- **Server CI test pipeline (uv, deps, pytest)** — _github_workflows_server_tests_server_tests_workflow, _github_workflows_server_tests_pytest_job, _github_workflows_server_tests_uv_toolchain, server_requirements_dev_dev_dependencies, server_requirements_runtime_dependencies, server_requirements_dev_pytest_asyncio [INFERRED 0.95]
- **Pre-post reel integrity checklist** — marketing_readme_silent_render_audio_note, marketing_readme_handwritten_suggestion_copy, marketing_readme_store_claim_consistency, marketing_readme_burned_in_captions [EXTRACTED 1.00]

## Communities (51 total, 9 thin omitted)

### Community 0 - "Onboarding & Demo Screens"
Cohesion: 0.07
Nodes (73): Alignment, AnswerOption, OnboardingQuestion, option(), HookNavigation(), DemoChat(), DemoLine, DemoScreen() (+65 more)

### Community 1 - "Remotion Reel Components"
Cohesion: 0.06
Nodes (49): Backdrop(), CaptionBar(), Eyebrow(), HookTitle(), ChatThumb(), HeartBurst(), ScreenFlash(), shutterScale() (+41 more)

### Community 2 - "Rizz Session State Machine"
Cohesion: 0.07
Nodes (31): ReplyError, BUSY, NO_CONNECTION, NO_SCREENSHOT, SERVER, SETUP, TIMEOUT, UNREADABLE (+23 more)

### Community 3 - "Keyboard IME Service"
Cohesion: 0.07
Nodes (19): UserPreferences, HookKeyboardService, Lifecycle, LifecycleOwner, StateFlow, KeyboardLifecycleOwner, Lifecycle, LifecycleOwner (+11 more)

### Community 4 - "Marketing Reel Design Rules"
Cohesion: 0.06
Nodes (40): server/** path-scoped CI trigger, pytest CI job, Server tests GitHub Actions workflow, uv Python toolchain (astral-sh/setup-uv), AllowanceChip free allowance, B object beat timeline, Burned-in captions, ui/theme/Color.kt palette (+32 more)

### Community 5 - "Paywall & Screen Routing"
Cohesion: 0.09
Nodes (36): CustomerCenter, Demo, Guide, Home, Onboarding, Paywall, Screen, Welcome (+28 more)

### Community 6 - "Web App Dependencies"
Cohesion: 0.06
Nodes (37): eslint, eslint-config-next, next, sharp, unrs-resolver, tailwindcss, @tailwindcss/postcss, @types/node (+29 more)

### Community 7 - "Android API Client"
Cohesion: 0.12
Nodes (17): AllowanceExhausted, ApiService, Failure, GeneratedReplies, GenerateRepliesResult, HookApiInterface, Bitmap, ConversationContext (+9 more)

### Community 8 - "Demo Chat Walkthrough"
Cohesion: 0.10
Nodes (24): ChatBubble(), ChatComposer(), ChatHeader(), Coach(), DemoChatStep(), DemoMessage, DemoPhase, PICK_REPLY (+16 more)

### Community 9 - "RevenueCat Billing Manager"
Cohesion: 0.14
Nodes (17): BillingManager, BillingOperation, PURCHASE, RESTORE, BillingOutcome, Cancelled, Failed, Idle (+9 more)

### Community 10 - "Remotion Project Manifest"
Cohesion: 0.06
Nodes (31): dependencies, react, react-dom, remotion, @remotion/cli, @remotion/google-fonts, description, devDependencies (+23 more)

### Community 11 - "Allowance Store Abstraction"
Cohesion: 0.09
Nodes (19): ABC, AllowanceStore, get_store(), _new_sqlite_store(), Free-generation allowance persistence. Variant A monetization: every install…, Durable store for production, backed by Supabase (PostgREST). Uses supabase-…, Connect on first use. Returns a fallback store if Supabase is out., Pick a store from the environment: Supabase when configured, else SQLite. (+11 more)

### Community 12 - "Web TypeScript Config"
Cohesion: 0.07
Nodes (28): dom.iterable, esnext, **/*.mts, .next/dev/types/**/*.ts, next-env.d.ts, .next/types/**/*.ts, node_modules, **/*.ts (+20 more)

### Community 13 - "FastAPI Route Handlers"
Cohesion: 0.13
Nodes (25): BaseModel, get, post, invalidate(), is_pro(), Is this install entitled to Hook Pro? Never raises: on any RevenueCat trouble…, Forget one install's cached answer so the next is_pro() re-asks RevenueCat. The…, _entitlement_snapshot() (+17 more)

### Community 14 - "Prompt & Context Building"
Cohesion: 0.16
Nodes (24): dict, build_context_block(), build_prompt(), ConversationContext, Session-only, hidden understanding of the whole conversation. Held by the…, Drop any <think> block and code fences a reasoning model may still emit., Render the prior context so the model can carry the conversation forward. Empty…, strip_reasoning() (+16 more)

### Community 15 - "Screenshot Detection Service"
Cohesion: 0.15
Nodes (10): Bitmap, ContentObserver, Intent, ScreenshotDetectionService, ContentObserver, BitmapFactory, IBinder, Notification (+2 more)

### Community 16 - "Entitlement Gating Tests"
Cohesion: 0.14
Nodes (21): client(), _fake_supabase_module(), _mock_revenuecat(), fixture, Tests for API-key auth, the free-generation allowance, and Pro entitlements.…, A throwaway in-memory allowance store for each test., ENTITLEMENT_ID is the preferred match, but this is a single-entitlement app, so…, store() (+13 more)

### Community 17 - "Marketing Site Pages"
Cohesion: 0.16
Nodes (10): metadata, Footer(), geistMono, geistSans, metadata, LegalPage(), Section(), metadata (+2 more)

### Community 18 - "SQLite Allowance Store"
Cohesion: 0.11
Nodes (9): _now_iso(), Single-connection SQLite store. sqlite3 is synchronous, so every statement runs…, SqliteAllowanceStore, _FakeChoice, _FakeCompletion, _FakeMessage, test_sqlite_store_counts_per_user(), test_sqlite_store_increments_are_not_lost_concurrently() (+1 more)

### Community 19 - "Generation Edge-Case Tests"
Cohesion: 0.21
Nodes (17): _body(), client(), _FakeChoice, _FakeCompletion, _FakeMessage, _headers(), _mock_groq(), _mock_is_pro() (+9 more)

### Community 20 - "Allowance & Auth Tests"
Cohesion: 0.24
Nodes (21): parametrize, _body(), _headers(), _mock_groq(), _mock_is_pro(), Local dev with no APP_API_KEY still works., Replace the Groq call. Returns a list that records each invocation., _spend() (+13 more)

### Community 21 - "RevenueCat Entitlement Cache"
Cohesion: 0.12
Nodes (17): AsyncClient, datetime, _CacheEntry, clear_cache(), entitlement_active(), _get_client(), _parse_rc_datetime(), RevenueCat entitlement lookups for the "Hook Pro" subscription. The Android app… (+9 more)

### Community 22 - "Remotion TypeScript Config"
Cohesion: 0.12
Nodes (16): compilerOptions, esModuleInterop, forceConsistentCasingInFileNames, jsx, lib, module, moduleResolution, noEmit (+8 more)

### Community 23 - "API DTOs & Conversation Session"
Cohesion: 0.15
Nodes (10): AllowanceExhaustedResponse, ConversationContext, ErrorResponse, GenerateRepliesRequest, GenerateRepliesResponse, MeResponse, PreferencesDto, ConversationSession (+2 more)

### Community 24 - "Preferences Repository"
Cohesion: 0.15
Nodes (4): UserPreferences, PreferencesKeys, PreferencesRepository, Flow

### Community 25 - "Groq Rate-Limit Backoff"
Cohesion: 0.20
Nodes (12): Exception, Seconds to wait after a Groq 429, from the header or the message text., retry_after_seconds(), _FakeRateLimitError, _FakeResponse, Exception, test_retry_after_seconds_falls_back_to_message_text(), test_retry_after_seconds_header_of_zero_becomes_one() (+4 more)

### Community 26 - "Supabase Store Test Doubles"
Cohesion: 0.17
Nodes (4): _FakeQuery, _FakeResponse, _FakeSupabaseClient, test_supabase_store_reads_and_increments()

### Community 27 - "Onboarding Persistence Tests"
Cohesion: 0.21
Nodes (12): _profile(), A client that lost the network mid-call retries; that must not stack rows., A question the app hasn't asked yet arrives as null, not as an error., The user has already finished; a store outage must not surface as an error., _saved_profile(), test_onboarding_accepts_a_partial_profile(), test_onboarding_is_stored(), test_onboarding_rejects_oversized_values() (+4 more)

### Community 28 - "Theme & Message Style Prefs"
Cohesion: 0.20
Nodes (7): MessageStyle, LOWERCASE, SENTENCE_CASE, ThemeMode, DARK, LIGHT, SYSTEM

### Community 29 - "Onboarding Field Enum"
Cohesion: 0.25
Nodes (8): OnboardingField, AGE_RANGE, FLIRT_LEVEL, GENDER, LOOKING_FOR, SEXUALITY, STYLE, TONE

### Community 30 - "Onboarding Sync"
Cohesion: 0.53
Nodes (4): OnboardingProfileRequest, Context, OnboardingSync, toOnboardingRequest()

### Community 31 - "Message Tone Enum"
Cohesion: 0.33
Nodes (5): MessageTone, FUNNY, GEN_Z_SLANG, RESPECTFUL, SMOOTH

### Community 32 - "Emoji Use Enum"
Cohesion: 0.40
Nodes (4): EmojiUse, EXPRESSIVE, MINIMAL, NEVER

### Community 33 - "Flirt Level Enum"
Cohesion: 0.40
Nodes (4): FlirtLevel, BOLD, LESS, MEDIUM

### Community 34 - "Reply Length Enum"
Cohesion: 0.40
Nodes (4): ReplyLength, EXTENDED, NORMAL, SHORT

### Community 35 - "Server API-Key Auth"
Cohesion: 0.40
Nodes (5): authenticate(), Shared gate for every metered route. Returns the caller's app user id., Constant-time shared-key check. Skipped entirely when unconfigured., require_api_key(), require_app_user_id()

### Community 37 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **192 isolated node(s):** `GenerateRepliesRequest`, `GenerateRepliesResponse`, `MeResponse`, `AllowanceExhaustedResponse`, `ErrorResponse` (+187 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PreferencesRepository` connect `Preferences Repository` to `Emoji Use Enum`, `Flirt Level Enum`, `Reply Length Enum`, `Keyboard IME Service`, `Android Application Entry`, `Onboarding & Demo Screens`, `Android API Client`, `RevenueCat Billing Manager`, `Screenshot Detection Service`, `Theme & Message Style Prefs`, `Onboarding Sync`, `Message Tone Enum`?**
  _High betweenness centrality (0.186) - this node is a cross-community bridge._
- **Why does `generate_replies()` connect `FastAPI Route Handlers` to `Groq Rate-Limit Backoff`, `Prompt & Context Building`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `ApiService` connect `Android API Client` to `Rizz Session State Machine`, `Keyboard IME Service`, `Android Application Entry`, `RevenueCat Billing Manager`, `Screenshot Detection Service`, `Preferences Repository`, `Onboarding Sync`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `SqliteAllowanceStore` (e.g. with `_FakeChoice` and `_FakeCompletion`) actually correct?**
  _`SqliteAllowanceStore` has 9 INFERRED edges - model-reasoned connections that need verification._
- **What connects `GenerateRepliesRequest`, `GenerateRepliesResponse`, `MeResponse` to the rest of the system?**
  _192 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Onboarding & Demo Screens` be split into smaller, more focused modules?**
  _Cohesion score 0.06554621848739496 - nodes in this community are weakly interconnected._
- **Should `Remotion Reel Components` be split into smaller, more focused modules?**
  _Cohesion score 0.05906553041434029 - nodes in this community are weakly interconnected._