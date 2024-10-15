#!/bin/bash

# Set up a local directory for installation
INSTALL_DIR=$HOME/Dependencies
mkdir -p $INSTALL_DIR

# Set up PATH and other environment variables to use local installations
export PATH="$INSTALL_DIR/bin:$PATH"
export LD_LIBRARY_PATH="$INSTALL_DIR/lib:$LD_LIBRARY_PATH"
export CPATH="$INSTALL_DIR/include:$CPATH"
export PKG_CONFIG_PATH="$INSTALL_DIR/lib/pkgconfig:$PKG_CONFIG_PATH"

# Install SQLite3 locally
cd $HOME
wget https://www.sqlite.org/2023/sqlite-autoconf-3410000.tar.gz  # Adjust version as needed
tar -xzvf sqlite-autoconf-3410000.tar.gz
cd sqlite-autoconf-3410000
./configure --prefix=$INSTALL_DIR
make -j$(nproc)
make install

# Verify SQLite3 installation
$INSTALL_DIR/bin/sqlite3 --version

# Install jemalloc locally
cd $HOME
wget https://github.com/jemalloc/jemalloc/releases/download/5.2.1/jemalloc-5.2.1.tar.bz2
tar -xjf jemalloc-5.2.1.tar.bz2
cd jemalloc-5.2.1
./configure --prefix=$INSTALL_DIR
make -j$(nproc)
make install

# Install CMake locally
cd $HOME
mkdir -p $HOME/temp && cd $HOME/temp
version=3.24
build=1
wget https://cmake.org/files/v$version/cmake-$version.$build.tar.gz
tar -xzvf cmake-$version.$build.tar.gz
cd cmake-$version.$build/
./bootstrap --prefix=$INSTALL_DIR
make -j$(nproc)
make install

# Ensure CMake is using the right version
$INSTALL_DIR/bin/cmake --version

# Clone and install gRPC and Protocol Buffers locally
cd $HOME
git clone --recurse-submodules -b v1.66.1 https://github.com/grpc/grpc 
cd grpc/third_party/protobuf
mkdir cmake/build
cd cmake/build
$INSTALL_DIR/bin/cmake ../..
make -j$(nproc)
make install DESTDIR=$INSTALL_DIR

cd $HOME/grpc
mkdir -p cmake/build
cd cmake/build
$INSTALL_DIR/bin/cmake ../..
make -j$(nproc)
make install DESTDIR=$INSTALL_DIR#!/bin/bash

# Set up a local directory for installation
INSTALL_DIR=$HOME/Dependencies
mkdir -p $INSTALL_DIR

# Set up PATH and other environment variables to use local installations
export PATH="$INSTALL_DIR/bin:$PATH"
export LD_LIBRARY_PATH="$INSTALL_DIR/lib:$LD_LIBRARY_PATH"
export CPATH="$INSTALL_DIR/include:$CPATH"
export PKG_CONFIG_PATH="$INSTALL_DIR/lib/pkgconfig:$PKG_CONFIG_PATH"

# Install SQLite3 locally
cd $HOME
wget https://www.sqlite.org/2023/sqlite-autoconf-3410000.tar.gz  # Adjust version as needed
tar -xzvf sqlite-autoconf-3410000.tar.gz
cd sqlite-autoconf-3410000
./configure --prefix=$INSTALL_DIR
make -j$(nproc)
make install

# Verify SQLite3 installation
$INSTALL_DIR/bin/sqlite3 --version

# Install jemalloc locally
cd $HOME
wget https://github.com/jemalloc/jemalloc/releases/download/5.2.1/jemalloc-5.2.1.tar.bz2
tar -xjf jemalloc-5.2.1.tar.bz2
cd jemalloc-5.2.1
./configure --prefix=$INSTALL_DIR
make -j$(nproc)
make install

# Install CMake locally
cd $HOME
mkdir -p $HOME/temp && cd $HOME/temp
version=3.24
build=1
wget https://cmake.org/files/v$version/cmake-$version.$build.tar.gz
tar -xzvf cmake-$version.$build.tar.gz
cd cmake-$version.$build/
./bootstrap --prefix=$INSTALL_DIR
make -j$(nproc)
make install

# Ensure CMake is using the right version
$INSTALL_DIR/bin/cmake --version

# Clone and install gRPC and Protocol Buffers locally
cd $HOME
git clone --recurse-submodules -b v1.66.1 https://github.com/grpc/grpc 
cd grpc/third_party/protobuf
mkdir cmake/build
cd cmake/build
$INSTALL_DIR/bin/cmake ../..
make -j$(nproc)
make install DESTDIR=$INSTALL_DIR

cd $HOME/grpc
mkdir -p cmake/build
cd cmake/build
$INSTALL_DIR/bin/cmake ../..
make -j$(nproc)
make install DESTDIR=$INSTALL_DIR