#pragma once

#include <android/log.h>

#ifndef BUILD_DEBUG
#define LOG_DEBUG(args...) __android_log_print(android_LogPriority::ANDROID_LOG_DEBUG, "NativeAudioPlayer", args)
#define LOG_INFO(args...) __android_log_print(android_LogPriority::ANDROID_LOG_INFO, "NativeAudioPlayer", args)
#define LOG_WARN(args...) __android_log_print(android_LogPriority::ANDROID_LOG_WARN, "NativeAudioPlayer", args)
#define LOG_ERROR(args...) __android_log_print(android_LogPriority::ANDROID_LOG_ERROR, "NativeAudioPlayer", args)
#else
#define LOG_DEBUG(args...)
#define LOG_INFO(args...)
#define LOG_WARN(args...)
#define LOG_ERROR(args...)
#endif
