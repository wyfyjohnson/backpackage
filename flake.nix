{
  description = "Create: Backpackage - A Minecraft mod adding Create-themed cardboard backpack";

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

            # Java Language Server for Helix
            jdt-language-server
          ];

          shellHook = ''
            echo "Create: Backpackage Development Environment"
            echo "Java Version: $(java -version 2>&1 | head -n 1)"
            echo ""
            echo "Available commands:"
            echo "  ./gradlew build          - Build the mod"
            echo "  ./gradlew runClient      - Run Minecraft client with mod"
            echo "  ./gradlew runServer      - Run Minecraft server with mod"
            echo "  ./gradlew runData        - Generate data (recipes, tags, etc.)"
            echo ""

            # Set JAVA_HOME for Gradle
            export JAVA_HOME="${jdk}"
          '';
        };
      }
    );
}
