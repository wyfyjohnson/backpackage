{
  description = "Create: Backpackage - A Minecraft mod adding Create-themed cardboard backpacks";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs { inherit system; };

        jdk = pkgs.jdk21;
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            # Java Development Kit 21 (required for Minecraft 1.21.1)
            jdk

            # Gradle wrapper is included in project, but this provides gradle commands
            gradle

            # Git for version control
            git

            # Text editor with Java support
            zed-editor

            # Java Language Server for Helix
            jdt-language-server

            # Nix Language Server
            nixd

            # Audio library for Minecraft client
            openal

            # Graphics libraries for Minecraft client
            libGL
            libGLU
            xorg.libX11
            xorg.libXext
            xorg.libXcursor
            xorg.libXrandr
            xorg.libXi
            xorg.libXxf86vm
          ];

          # Set library path for OpenGL
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
            pkgs.openal
            pkgs.libGL
            pkgs.libGLU
            pkgs.xorg.libX11
            pkgs.xorg.libXext
            pkgs.xorg.libXcursor
            pkgs.xorg.libXrandr
            pkgs.xorg.libXi
            pkgs.xorg.libXxf86vm
          ];

          shellHook = ''
            echo "Create: Backpackage Development Environment"
            echo "Java Version: $(java -version 2>&1 | head -n 1)"
            echo "JAVA_HOME: ${jdk}"
            echo ""
            echo "Available commands:"
            echo "  zed .                    - Open project in Zed editor"
            echo "  ./gradlew build          - Build the mod"
            echo "  ./gradlew runClient      - Run Minecraft client with mod"
            echo "  ./gradlew runServer      - Run Minecraft server with mod"
            echo "  ./gradlew runData        - Generate data (recipes, tags, etc.)"
            echo ""

            # Set JAVA_HOME for Gradle and IDEs
            export JAVA_HOME="${jdk}"

            # Use zsh if available
            if [ -n "$ZSH_VERSION" ]; then
              return
            elif command -v zsh &> /dev/null; then
              exec zsh
            fi
          '';
        };
      }
    );
}
