#include <jni.h>
#include <memory>

#include "logging.h"
#include "audio_player.h"
#include "jni_bridge.h"

#define TAG "NativeAudioPlayer"

AudioPlayer::AudioPlayer() {
    // Thread safety is handled in the Kotlin class
    createStream();
}

AudioPlayer::~AudioPlayer() {
    stop();
}

oboe::AudioStreamBuilder& AudioPlayer::getBuilder() {
    static oboe::AudioStreamBuilder builder;
    builder.setPerformanceMode(oboe::PerformanceMode::None)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setDataCallback(this)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setDirection(oboe::Direction::Output)
            ->setUsage(oboe::Usage::Game)
            ->setContentType(oboe::ContentType::Music);
    return builder;
}

void AudioPlayer::createStream() {
    if (stream) {
        LOG_WARN(TAG, "createStream: AudioStream already exists");
        return;
    }

    oboe::Result result = getBuilder().openStream(stream);
    if (result != oboe::Result::OK) {
        LOG_ERROR(TAG, "Failed to create AudioStream. Error: %s", oboe::convertToText(result));
        return;
    }
    LOG_DEBUG(TAG, "AudioStream created");
}

void AudioPlayer::start() {
    if (!stream) {
        createStream();
    }

    stream->requestStart();
    LOG_DEBUG(TAG, "AudioStream start requested");
}

void AudioPlayer::pause() {
    if (!stream) {
        LOG_WARN(TAG, "pause: AudioStream doesn't exist");
        return;
    }

    stream->requestPause();
    LOG_DEBUG(TAG, "AudioStream pause requested");
}

void AudioPlayer::stop() {
    if (!stream) {
        LOG_WARN(TAG, "stop: AudioStream doesn't exist");
        return;
    }

    stream->stop();
    stream->close();
    stream.reset();
    LOG_DEBUG(TAG, "stop: AudioStream stopped");
}

int AudioPlayer::getSampleRate() {
    if (!stream) {
        LOG_WARN(TAG, "getSampleRate: AudioStream doesn't exist");
        return 44100;
    }

    return stream->getSampleRate();
}

oboe::DataCallbackResult
AudioPlayer::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    auto *outputData = static_cast<float *>(audioData);

    int numSamples = requestSamples(numFrames, outputData);
    if (numSamples < numFrames) {
        for (int i = numSamples; i < numFrames; i++) {
            outputData[i] = 0.0f;
        }
    }

    //LOG_DEBUG(TAG, "Number of samples requested: %d, got: %d", numFrames, numSamples);

    return oboe::DataCallbackResult::Continue;
}
