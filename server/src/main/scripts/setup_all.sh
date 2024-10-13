#!/bin/bash

# Set up a local directory for installation
INSTALL_DIR=$HOME/Dependencies
mkdir -p $INSTALL_DIR

# Set up PATH and other environment variables to use local installations
export PATH="$INSTALL_DIR/bin:$PATH"
export LD_LIBRARY_PATH="$INSTALL_DIR/lib:$LD_LIBRARY_PATH"
export CPATH="$INSTALL_DIR/include:$CPATH"
export PKG_CONFIG_PATH="$INSTALL_DIR/lib/pkgconfig:$PKG_CONFIG_PATH"

# Install jemalloc locally
echo "Installing jemalloc..."
cd $HOME
wget https://github.com/jemalloc/jemalloc/releases/download/5.2.1/jemalloc-5.2.1.tar.bz2
tar -xjf jemalloc-5.2.1.tar.bz2
cd jemalloc-5.2.1
./configure --prefix=$INSTALL_DIR
make -j$(nproc)
make install
cd ..

# Install CMake locally
echo "Installing CMake..."
mkdir -p $HOME/temp && cd $HOME/temp
version=3.24
build=1
wget https://cmake.org/files/v$version/cmake-$version.$build.tar.gz
tar -xzvf cmake-$version.$build.tar.gz
cd cmake-$version.$build/
./bootstrap --prefix=$INSTALL_DIR
make -j$(nproc)
make install
cd $HOME

# Ensure CMake is using the right version
cmake --version

# Install gRPC and Protobuf dependencies
echo "Cloning and installing gRPC and Protobuf..."
git clone --recurse-submodules -b v1.66.1 https://github.com/grpc/grpc 
cd grpc/third_party/protobuf
mkdir cmake/build
cd cmake/build
cmake ../..
make -j 8
sudo make install

cd ~/grpc  # Go back to the gRPC root folder
mkdir -p cmake/build
cd cmake/build
cmake ../..
make -j 8
sudo make install

# Go back to the root directory
cd $HOME

echo "Setup completed!"
