# ADR-0001: Hilt for ViewModel injection

- **Status:** Accepted (2026-10-06)
- **Context:** Slice 4 introduces the first ViewModel (timer screen). Choice: Hilt (`hilt-android` 2.60.1, KSP `2.2.21-2.0.5` matching Kotlin 2.2.x) vs manual `AppContainer`.
- **Decision:** Adopt Hilt now. It is Google's recommended DI, the industry default Rheniel will meet in jobs, and the UUPM compose rule 38 prescribes it. Single `:app` module stays (rules 44–45: modularize on a real trigger, not day one).
- **Consequences:** KSP + Hilt Gradle plugins, `StudoraApp : Application @HiltAndroidApp`, `@AndroidEntryPoint` on MainActivity, `@HiltViewModel` VMs. Never mix with a manual container. Build pays KSP codegen per compile — accepted.
