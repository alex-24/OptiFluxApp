//
// Created by Alexis Cassion on 16/10/2025.
//

#ifndef OPTIFLUXAPP_OPTICAL_FLOW_CALCULATOR_CORE_H
#define OPTIFLUXAPP_OPTICAL_FLOW_CALCULATOR_CORE_H

#endif //OPTIFLUXAPP_OPTICAL_FLOW_CALCULATOR_CORE_H

#include <vector>
#include <cstdint>

struct OpticalFlowVector {
    float x;
    float y;
    float u;
    float v;
    float magnitude;
};

struct OpticalFlowParams {
    int frameWidth;
    int frameHeight;
    int cellSize;
    int windowSize;
};

bool setParams(const OpticalFlowParams& params);

void computeFlow(
        const uint8_t* previousImage,
        int previousFrameWidth,
        const uint8_t* currentImage,
        int currentFrameWidth,
        std::vector<OpticalFlowVector>& out);