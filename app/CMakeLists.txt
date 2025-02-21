cmake_minimum_required(VERSION 3.4.1)

project(NesEmuAndroid)

add_library(audio-player SHARED src/main/cpp/audio-player.cpp)
include_directories(src/main/cpp/)

find_library(log-lib log)
find_package(oboe REQUIRED CONFIG)

target_link_libraries(audio-player ${log-lib} oboe::oboe)
