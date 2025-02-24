#pragma once

#include <queue>
#include <oboe/Oboe.h>

class AudioPlayer : public oboe::AudioStreamDataCallback {
public:
    AudioPlayer();
    ~AudioPlayer() override;

private:
    std::shared_ptr<oboe::AudioStream> stream;

    oboe::AudioStreamBuilder& getBuilder();
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;
    void createStream();

public:
    void start();
    void pause();
    void stop();
    int getSampleRate();
};