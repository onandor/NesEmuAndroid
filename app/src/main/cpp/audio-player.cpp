#include <jni.h>
#include <memory>

#include "logging.h"
#include "audio-player.hpp"

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_onandor_nesemu_audio_AudioPlayer_create(JNIEnv* env, jobject obj) {
        auto audioPlayer = std::make_unique<audioplayer::AudioPlayer>();
        if (!audioPlayer) {
            LOG_ERROR("Failed to create AudioPlayer instance");
            audioPlayer.reset(nullptr);
        }

        return reinterpret_cast<jlong>(audioPlayer.release());
    }

    JNIEXPORT void JNICALL
    Java_com_onandor_nesemu_audio_AudioPlayer_delete(
            JNIEnv* env,
            jobject obj,
            jlong handle) {
        auto* audioPlayer = reinterpret_cast<audioplayer::AudioPlayer*>(handle);
        if (!audioPlayer) {
            LOG_WARN("delete: AudioPlayer instance is uninitialized");
            return;
        }

        delete audioPlayer;
    }

    JNIEXPORT void JNICALL
    Java_com_onandor_nesemu_audio_AudioPlayer_playSound(
            JNIEnv* env,
            jobject obj,
            jlong handle) {
        auto* audioPlayer = reinterpret_cast<audioplayer::AudioPlayer*>(handle);
        if (!audioPlayer) {
            LOG_WARN("playSound: AudioPlayer instance is uninitialized");
            return;
        }

        audioPlayer->playSound();
    }
}

namespace audioplayer {
    void AudioPlayer::playSound() {
        LOG_INFO("Hello World! Playing a sound :)");
    }
}
