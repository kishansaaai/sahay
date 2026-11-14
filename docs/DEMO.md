# Demo video script (B22)

Target: 4 to 5 minutes, recorded on the phone in airplane mode, captions in English and Hindi. Nothing in this script has been recorded yet.

## Before recording

- Install the debug APK from the CI artifact (see [TESTING.md](TESTING.md)) and note its SHA-256.
- Turn on **airplane mode** before the first shot and keep the status bar visible for the whole recording.
- If no `.pte` is on the phone, the app labels explanations and tutor replies as template output. Do not hide that: show the label and say what it means. A demo of template output is honest; a demo that implies model output would not be.
- Reset the app between takes (Settings → Delete all data) so the intake flow is real.
- Record at 1080p, 30 fps, with system sounds optionally on; keep the phone at a readable font size.
- Do not read out benchmark numbers that are not in `bench/` or `docs/results/`.

## Shot list

| Time | Screen | Action | Narration (English; Hindi caption follows) |
| --- | --- | --- | --- |
| 0:00-0:20 | Home screen, airplane mode on | Show the toggle, open Sahay | "Sahay is an on-device study companion. The phone is in airplane mode and the app has no network permission." |
| 0:20-0:55 | Welcome → Intake | Type a goal, answer hours/deadline/interests, create the profile | "It asks for a goal in plain words, then the time available." |
| 0:55-1:40 | Diagnostic | Answer three or four questions; show the hint ladder once and the confidence slider | "A short adaptive diagnostic estimates what is already known, so the plan starts in the right place." |
| 1:40-2:20 | Roadmap | Scroll weeks, open "why this" on one item | "Every item has a reason and a prerequisite order. Nothing is generated that the phone cannot explain." |
| 2:20-2:55 | Today → Study | Open the next action, read the explanation and analogy, answer the check question | "The next action is one tap away. The explanation is grounded in the skill notes." (If template output, point at the label here.) |
| 2:55-3:20 | Today | Show mastery and streak changing after the answer | "Mastery and the plan update from the answer, not from a fixed script." |
| 3:20-3:45 | Dashboard | Show per-skill bars and the forecast or its no-trend state | "Where there is not enough history, it says so instead of inventing a date." |
| 3:45-4:10 | Settings → Share | Show the plan QR, then import it on the same phone | "A plan can move between phones as a QR code, still offline." |
| 4:10-4:30 | Close | Back to home, airplane mode still visible | "Everything shown ran on this phone, with no network." |

## Hindi captions

Write captions from the same script rather than translating word for word; keep numbers and screen names as they appear in the Hindi UI. The Hindi strings are in `app/src/main/res/values-hi/strings.xml`; if a caption and a label disagree, change the caption.

## After recording

- Put the file link in the README and in issue #41; state the phone model, Android version, APK SHA-256 and the fact that airplane mode was on.
- Cut anything that shows a result you cannot reproduce. A missing shot is better than a staged one.
- If a shot fails (for example OCR is not implemented), say so on camera and move on; do not substitute stock footage.
