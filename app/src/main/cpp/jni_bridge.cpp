#include <jni.h>

#include "jni_bridge.h"
#include "audio_player.h"
#include "logging.h"

#define TAG "JniBridge"

JavaVM *gJvm = nullptr;
jmethodID gRequestSamplesCallback = nullptr;
jobject gJvmAudioPlayerInstance = nullptr;

JNIEnv *getJNIEnv() {
    JNIEnv *env = nullptr;
    int status = gJvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        status = gJvm->AttachCurrentThread(&env, nullptr);
    }
    if (status != JNI_OK) {
        LOG_ERROR(TAG, "Unable to retrieve JNI environment context");
        return nullptr;
    }
    return env;
}

int requestSamples(int numSamples, int16_t *outputData) {
    JNIEnv *env = getJNIEnv();
    jobject buffer = env->NewDirectByteBuffer(outputData, numSamples * sizeof(int16_t));
    int numSamplesGot = env->CallIntMethod(gJvmAudioPlayerInstance,
                                           gRequestSamplesCallback,
                                           buffer);
    env->DeleteLocalRef(buffer);
    gJvm->DetachCurrentThread();
    return numSamplesGot;
}

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
        gJvm = jvm;
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL
    Java_com_onandor_nesemu_domain_audio_AudioPlayer_create(JNIEnv* env, jobject obj) {
        auto audioPlayer = std::make_unique<AudioPlayer>();
        if (!audioPlayer) {
            LOG_ERROR(TAG, "create: Failed to create AudioPlayer instance");
            audioPlayer.reset(nullptr);
        }

        if (gJvmAudioPlayerInstance != nullptr) {
            env->DeleteGlobalRef(gJvmAudioPlayerInstance);
        }
        gJvmAudioPlayerInstance = env->NewGlobalRef(obj);
        if (!gJvmAudioPlayerInstance) {
            LOG_ERROR(TAG, "create: Failed to acquire JVM AudioPlayer instance");
            audioPlayer.reset(nullptr);
        }

        if (!gRequestSamplesCallback) {
            jclass jAudioPlayerClass = env->FindClass("com/onandor/nesemu/domain/audio/AudioPlayer");
            gRequestSamplesCallback = env->GetMethodID(jAudioPlayerClass,
                                                       "onSamplesRequested",
                                                       "(Ljava/nio/ByteBuffer;)I");
            env->DeleteLocalRef(jAudioPlayerClass);
            if (!gRequestSamplesCallback) {
                LOG_ERROR(TAG, "create: Failed to acquire JVM callback method");
                env->DeleteGlobalRef(gJvmAudioPlayerInstance);
                gJvmAudioPlayerInstance = nullptr;
                audioPlayer.reset(nullptr);
            }
        }

        return reinterpret_cast<jlong>(audioPlayer.release());
    }

    JNIEXPORT void JNICALL
    Java_com_onandor_nesemu_domain_audio_AudioPlayer_delete(
            JNIEnv* env,
            jobject obj,
            jlong handle) {
        auto* audioPlayer = reinterpret_cast<AudioPlayer*>(handle);
        if (!audioPlayer) {
            LOG_WARN(TAG, "delete: AudioPlayer instance is uninitialized");
            return;
        }

        audioPlayer->stop();
        delete audioPlayer;

        env->DeleteGlobalRef(gJvmAudioPlayerInstance);
        gJvmAudioPlayerInstance = nullptr;
    }

    JNIEXPORT void JNICALL
    Java_com_onandor_nesemu_domain_audio_AudioPlayer_startStream(
            JNIEnv* env,
            jobject obj,
            jlong handle) {
        auto* audioPlayer = reinterpret_cast<AudioPlayer*>(handle);
        if (!audioPlayer) {
            LOG_WARN(TAG, "start: AudioPlayer instance is uninitialized");
            return;
        }

        audioPlayer->start();
    }

    JNIEXPORT void JNICALL
    Java_com_onandor_nesemu_domain_audio_AudioPlayer_pauseStream(
            JNIEnv* env,
            jobject obj,
            jlong handle) {
        auto* audioPlayer = reinterpret_cast<AudioPlayer*>(handle);
        if (!audioPlayer) {
            LOG_WARN(TAG, "pause: AudioPlayer instance is uninitialized");
            return;
        }

        audioPlayer->pause();
    }

    JNIEXPORT jint JNICALL
    Java_com_onandor_nesemu_domain_audio_AudioPlayer_getSampleRate(
            JNIEnv* env,
            jobject obj,
            jlong handle) {
        auto* audioPlayer = reinterpret_cast<AudioPlayer*>(handle);
        if (!audioPlayer) {
            return -1;
        }

        return audioPlayer->getSampleRate();
    }
}