# ADR-0003: navigation-compose for Timer | History tabs

- **Status:** Accepted (2026-10-06)
- **Context:** Heatmap needs its own surface (Rheniel chose separate tab over same-screen). Choice: navigation-compose vs manual screen-state switch.
- **Decision:** navigation-compose 2.10.2 (latest stable). Two destinations, sealed `Route` objects (CSV rule 17), bottom `NavigationBar`. Timer owns its VM; History owns its VM — ticker can never touch heatmap.
- **Consequences:** New catalog version + library. NavHost in MainActivity. Heatmap grid lands in slice 6b on the History destination. Single `:app` stays.
