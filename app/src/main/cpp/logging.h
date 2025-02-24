#pragma once

#include <android/log.h>

#ifdef BUILD_DEBUG
#define LOG_DEBUG(tag, args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_DEBUG, tag, args)
#define LOG_INFO(tag, args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_INFO, tag, args)
#define LOG_WARN(tag, args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_WARN, tag, args)
#define LOG_ERROR(tag, args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_ERROR, tag, args)
#else
#define LOG_DEBUG(tag, args...)
#define LOG_INFO(tag, args...)
#define LOG_WARN(tag, args...)
#define LOG_ERROR(tag, args...)
#endif
