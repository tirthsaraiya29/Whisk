# Whisk Design System & Philosophy

Whisk is built with a quiet, utilitarian, and high-performance aesthetic, aiming to be a desktop-class developer tool on a mobile device.

## Design Philosophy

- **Zero Clutter**: The interface should get out of the way of the user's thoughts and code. Menus are hidden until needed, and the bottom navigation is compact.
- **Speed & Efficiency**: Everything must be accessible instantly. The Command Palette (`Ctrl/Cmd + K`) is the primary navigation paradigm for power users.
- **Developer-Centric**: Markdown, code blocks, and knowledge graphs take precedence over flashy animations.

## Theming

Whisk supports a dual-mode theme system built on Material 3:

### 1. Warm Light Theme
Designed for daytime focus and natural reading.
- **Base Background**: Soft beige (`#FBF9F6`)
- **Surfaces**: Clean, slightly elevated white or off-white cards.
- **Typography**: Deep slate for high readability without the harshness of pure black.

### 2. Charcoal Dark Theme
Designed for low-light environments and long coding sessions.
- **Base Background**: Deep matte charcoal (`#131417`)
- **Surfaces**: Subtle, low-contrast elevations.
- **Typography**: Muted slate and soft white accents.

## Key UI Components

- **Haze Glass Blur**: Used sparingly for overlays, dialogs, and bottom sheets (`dev.chrisbanes.haze`) to provide context without obscuring the spatial canvas or knowledge graph.
- **Dual-Mode Editor**: Seamless transition between raw text and WYSIWYG preview. Code blocks use syntax highlighting native to the language.
- **Knowledge Graph View**: A highly interactive 2D physics-based node layout. Uses distinctive colors/shapes to separate Folders, Tags, and Notes.

## Software Architecture Design

Whisk enforces strict separation of concerns using Clean Architecture:
- **Presentation (Jetpack Compose)**: Stateless UI, driven by `StateFlow` from ViewModels.
- **Domain**: Use Cases / Interactors containing core business logic (e.g., link resolution, checklist aggregation).
- **Data (Room + DataStore)**: Local-first persistence. The source of truth is always the local Room database, ensuring 0ms latency for reads/writes.
- **AI Integration (LiteRT-LM)**: AI logic operates asynchronously in background workers or dedicated Coroutine scopes to prevent UI jank during inference.