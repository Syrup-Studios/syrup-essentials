{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  buildInputs = with pkgs; [
    jdk21
  ];
  shellHook = ''
    export JAVA_HOME=${pkgs.jdk21}
    echo "Fabric 1.20.1 Build Environment Ready."
  '';
}