cmake_minimum_required(VERSION 3.4.1)

project(NesEmuAndroid)

add_library(
        audio-player
        SHARED
        src/main/cpp/audio_player.cpp
        src/main/cpp/jni_bridge.cpp
)
include_directories(src/main/cpp/)

find_library(log-lib log)
find_package(oboe REQUIRED CONFIG)

if (CMAKE_BUILD_TYPE STREQUAL "debug")
    add_definitions(-DBUILD_DEBUG)
endif()

target_link_libraries(audio-player ${log-lib} oboe::oboe)
