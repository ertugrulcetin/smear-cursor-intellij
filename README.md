# Smear Cursor for IntelliJ IDEA

**Animate the cursor with a smear effect in IntelliJ IDEA and other JetBrains IDEs.**

Inspired by [Neovide's animated cursor](https://neovide.dev/features.html#animated-cursor) and the [smear-cursor.nvim](https://github.com/sphamba/smear-cursor.nvim) Neovim plugin.

[Demo](https://private-user-images.githubusercontent.com/17217484/389300116-fc95b4df-d791-4c53-9141-4f870eb03ab2.mp4?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzI0NzY0NDAsIm5iZiI6MTczMjQ3NjE0MCwicGF0aCI6Ii8xNzIxNzQ4NC8zODkzMDAxMTYtZmM5NWI0ZGYtZDc5MS00YzUzLTkxNDEtNGY4NzBlYjAzYWIyLm1wND9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDExMjQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQxMTI0VDE5MjIyMFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTg1NjFhZjJlODQ4YmU2NjAzY2EzY2I3NWMzMzI5MWQ1Njk2MTExYmEwYmExNTMwMThmYTJjYjE2ZjIyOThjNjMmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.Skw2VVyVWVkMe4ht6mvl_AZ_6QasJm8O6qsIZmcQ2XE)

## Features

- **Smooth cursor animation** with spring physics
- **Configurable animation speed** - adjust stiffness, damping, and trailing behavior
- **Color gradient trail effect** - beautiful fading trail following your cursor
- **Optional particle effects** - add sparkle to your cursor movement
- **Works across all editor windows** - consistent experience everywhere

## Installation

### From JetBrains Marketplace

1. Open IntelliJ IDEA
2. Go to **Settings/Preferences → Plugins → Marketplace**
3. Search for "Smear Cursor"
4. Click **Install**
5. Restart the IDE

### Manual Installation

1. Download the latest release from the [Releases](https://github.com/yourusername/smear-cursor-intellij/releases) page
2. Go to **Settings/Preferences → Plugins**
3. Click the gear icon → **Install Plugin from Disk...**
4. Select the downloaded `.zip` file
5. Restart the IDE

## Configuration

Go to **Settings/Preferences → Editor → Smear Cursor** to configure:

### General Settings
- **Enable Smear Cursor** - Toggle the effect on/off
- **Smear between windows** - Animate when switching between editor windows
- **Smear between neighbor lines** - Animate for small vertical movements
- **Direction toggles** - Enable/disable horizontal, vertical, and diagonal smearing

### Animation Settings
- **Stiffness** - How fast the cursor head moves towards target (0-100%)
- **Trailing Stiffness** - How fast the cursor tail follows (0-100%)
- **Damping** - Velocity reduction over time (0-100%)
- **Anticipation** - Initial "bounce back" effect (0-50%)
- **Max Length** - Maximum trail length in characters
- **Frame Interval** - Animation timing in milliseconds

### Particle Effects
- **Enable particles** - Add particle effects to cursor trail
- **Max Particles** - Maximum number of particles
- **Particle Lifetime** - How long particles last in milliseconds

### Color Settings
- **Use editor cursor color** - Match the editor's caret color
- **Custom cursor color** - Set a specific color for the trail

## Quick Toggle

- Right-click in the editor and select **Toggle Smear Cursor**
- Or use the action: `Toggle Smear Cursor` (searchable via Ctrl+Shift+A / Cmd+Shift+A)

## Building from Source

### Requirements
- JDK 17+
- IntelliJ IDEA 2023.3+

### Build
```bash
./gradlew build
```

### Run in Development
```bash
./gradlew runIde
```

### Package Plugin
```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`.

## Configuration Examples

### Faster Smear
For a snappier animation:
- Stiffness: 80%
- Trailing Stiffness: 60%
- Damping: 95%

### Smooth Cursor (No Trail)
For a smooth cursor without the trail effect:
- Stiffness: 50%
- Trailing Stiffness: 50%
- Max Length: 2

### Fire Hazard Mode 🔥
For maximum visual effect:
- Enable particles
- Stiffness: 50%
- Trailing Stiffness: 20%
- Damping: 60%
- Max Particles: 150
- Particle Lifetime: 500ms

## Known Issues

- May have reduced performance with very large files
- Animation may pause briefly during heavy IDE operations

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Credits

- Original concept: [Neovide](https://neovide.dev/)
- Neovim implementation: [smear-cursor.nvim](https://github.com/sphamba/smear-cursor.nvim) by sphamba
- IntelliJ IDEA port: This project
