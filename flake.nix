{
  description = "PsychonautWiki Journal Desktop Development Environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        
        jdk = pkgs.openjdk17;
        
        buildInputs = with pkgs; [
          # JVM and Kotlin development
          jdk
          gradle_7
          kotlin
          
          # Desktop development dependencies
          pkg-config
          cmake
          ninja
          
          # Graphics and windowing libraries
          wayland
          wayland-protocols
          libxkbcommon
          libGL
          mesa
          xorg.libX11
          xorg.libXext
          xorg.libXi
          xorg.libXrandr
          xorg.libXrender
          xorg.libXtst
          xorg.libXxf86vm
          libdrm
          
          # Development tools
          git
          direnv
          sqlite
          
          # Testing and CI
          docker
          docker-compose
        ];
        
        nativeBuildInputs = with pkgs; [
          pkg-config
          cmake
          ninja
        ];
        
      in
      {
        devShells.default = pkgs.mkShell {
          inherit buildInputs nativeBuildInputs;
          
          JAVA_HOME = "${jdk}";
          GRADLE_OPTS = "-Dorg.gradle.daemon=false";
          
          # Wayland environment variables
          WAYLAND_DISPLAY = "wayland-1";
          XDG_SESSION_TYPE = "wayland";
          
          shellHook = ''
            echo "🚀 PsychonautWiki Journal Desktop Development Environment"
            echo "Java: $(java -version 2>&1 | head -n 1)"
            echo "Gradle: $(gradle --version | grep Gradle)"
            echo "Kotlin: $(kotlin -version 2>&1)"
            echo ""
            echo "Available commands:"
            echo "  gradle build          - Build the desktop application"
            echo "  gradle run            - Run the desktop application"
            echo "  gradle test           - Run tests"
            echo "  gradle check          - Run all checks including BDD tests"
            echo ""
            
            # Set up graphics library paths for Compose Desktop
            export LD_LIBRARY_PATH="${pkgs.libGL}/lib:${pkgs.mesa}/lib:${pkgs.xorg.libX11}/lib:${pkgs.xorg.libXext}/lib:${pkgs.xorg.libXi}/lib:${pkgs.xorg.libXrandr}/lib:${pkgs.xorg.libXrender}/lib:${pkgs.xorg.libXtst}/lib:${pkgs.xorg.libXxf86vm}/lib:${pkgs.libdrm}/lib:$LD_LIBRARY_PATH"
            
            # Set up direnv if .envrc exists
            if [ -f .envrc ]; then
              echo "🔧 direnv detected, environment will auto-load"
            fi
          '';
        };
        
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "psychonautwiki-journal-desktop";
          version = "1.0.0";
          
          src = ./.;
          
          inherit buildInputs nativeBuildInputs;
          
          buildPhase = ''
            gradle build
          '';
          
          installPhase = ''
            mkdir -p $out/bin
            cp -r build/libs/* $out/bin/
          '';
        };
        
        checks = {
          build = self.packages.${system}.default;
          
          tests = pkgs.stdenv.mkDerivation {
            pname = "psychonautwiki-journal-tests";
            version = "1.0.0";
            
            src = ./.;
            
            inherit buildInputs nativeBuildInputs;
            
            buildPhase = ''
              gradle test
              gradle integrationTest
            '';
            
            installPhase = ''
              mkdir -p $out
              cp -r build/reports $out/
            '';
          };
        };
      });
}