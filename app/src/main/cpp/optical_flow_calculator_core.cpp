//
// Created by Alexis Cassion on 16/10/2025.
//

#include "optical_flow_calculator_core.h"
#include <cmath>
#include <algorithm>

static OpticalFlowParams PARAMS;

// calc the first derivative of the.. image => values are discrete so we take the difference of the intensity
// of the immediate neighbors on the given axis
static void sobel(
        const uint8_t* image,
        int w,
        int h,
        int stride,
        std::vector<float>& gx,
        std::vector<float>& gy) {

    gx.assign(w*h, 0.f);
    gy.assign(w*h, 0.f);


    for (int y=1; y<h-1; ++y) {
        for (int x=1; x<w-1; ++x) {
            int idx = y*w + x;
            int xm1 = (y  )*stride + (x-1);
            int xp1 = (y  )*stride + (x+1);
            int ym1 = (y-1)*stride + x;
            int yp1 = (y+1)*stride + x;
            float gxv = -image[ym1-1] - 2*image[xm1] - image[yp1-1]+ image[ym1+1] + 2*image[xp1] + image[yp1+1];
            float gyv =  image[ym1-1] + 2*image[ym1] + image[ym1+1]- image[yp1-1] - 2*image[yp1] - image[yp1+1];

            gx[idx] = gxv * (1.0f/8.0f);
            gy[idx] = gyv * (1.0f/8.0f);
        }
    }
}

static inline float clamp_det (float a, float b, float  c) {
    float det = a*c - b*b;

    if (std::fabs(det) < 1e-6f) {
        return (det<0) ? -1e-6f : 1e-6f;
    } else {
        return det;
    }
}

bool setParams(const OpticalFlowParams& params) {
    PARAMS = params;
    return true;
}

void computeFlow(
        const uint8_t* previousImage,
        int previousFrameWidth,
        const uint8_t* currentImage,
        int currentFrameWidth,
        std::vector<OpticalFlowVector>& out) {

    const int w = PARAMS.frameWidth;
    const int h = PARAMS.frameHeight;

    std::vector<float> Ix;
    std::vector<float> Iy;

    sobel(currentImage, w, h, currentFrameWidth, Ix, Iy);

    out.clear();
    const int r = PARAMS.windowSize;
    for (int y = r; y < h - r; y += PARAMS.cellSize) {
        for (int x = r; x < w - r; x += PARAMS.cellSize) {
            // Accumulate within window
            float A11=0, A12=0, A22=0, B1=0, B2=0;
            for (int dy=-r; dy<=r; ++dy) {
                const int yy = y+dy;
                for (int dx=-r; dx<=r; ++dx) {
                    const int xx = x+dx;
                    const int idx = yy*w + xx;
                    float ix = Ix[idx];
                    float iy = Iy[idx];
                    float it = float(currentImage[yy*currentFrameWidth + xx]) - float(previousImage[yy*previousFrameWidth + xx]);
                    A11 += ix*ix;
                    A12 += ix*iy;
                    A22 += iy*iy;
                    B1  += ix*it;
                    B2  += iy*it;
                }
            }
            float det = clamp_det(A11, A12, A22);
            float inv11 =  A22 / det;
            float inv12 = -A12 / det;
            float inv22 =  A11 / det;
            float u = -(inv11*B1 + inv12*B2);
            float v = -(inv12*B1 + inv22*B2);
            float mag = std::sqrt(u*u + v*v);
            out.push_back({(float)x, (float)y, u, v, mag});
        }
    }
}