//
// Created by Alexis Cassion on 16/10/2025.
//


#include <jni.h>
#include "optical_flow_calculator_core.h"

static jfloatArray pack(
        JNIEnv* env,
        const std::vector<OpticalFlowVector>& v) {

    jfloatArray arr = env->NewFloatArray((jsize)(v.size()*5));

    std::vector<float> tmp;
    tmp.reserve(v.size()*5);

    for (auto& f : v) { tmp.push_back(f.x); tmp.push_back(f.y);
        tmp.push_back(f.u); tmp.push_back(f.v); tmp.push_back(f.magnitude); }
    env->SetFloatArrayRegion(arr, 0, (jsize)tmp.size(), tmp.data());

    return arr;
}

extern "C" JNIEXPORT jboolean
Java_com_acassion_optifluxapp_native_NativeOpticalFlowCalculator_setParams(
        JNIEnv* env,
        jobject thiz,
        jint frameWidth,
        jint frameHeight,
        jint cellSize,
        jint windowSize) {

    OpticalFlowParams params {
        frameWidth,
        frameHeight,
        cellSize,
        windowSize
    };
    return setParams(params);
}

extern "C" JNIEXPORT jfloatArray
Java_com_acassion_optifluxapp_native_NativeOpticalFlowCalculator_compute(
        JNIEnv* env,
        jobject thiz,
        jbyteArray prevArr,
        jbyteArray currArr,
        jint previousFrameWidth,
        jint currentFrameWidth) {

    jbyte* pp = env->GetByteArrayElements(prevArr, nullptr);
    jbyte* cp = env->GetByteArrayElements(currArr, nullptr);

    std::vector<OpticalFlowVector> out;
    computeFlow(
            reinterpret_cast<const uint8_t *>(pp),
            previousFrameWidth,
            reinterpret_cast<uint8_t*>(cp),
            currentFrameWidth,
            out);

    env->ReleaseByteArrayElements(prevArr, pp, JNI_ABORT);
    env->ReleaseByteArrayElements(currArr, cp, JNI_ABORT);
    return pack(env, out);
}
